package me.taks.proto.ts;

import me.taks.proto.Field;
import me.taks.proto.Field.Options;
import me.taks.proto.Field.Scope;
import me.taks.proto.ts.TypeScriptRenderer.Chain;

class MessageItemDecoder {
	public final Field item;

	public MessageItemDecoder(Field item) {
		this.item = item;
	}

	public String decoder() {
		return Chain.chain(unprocessedDecoder())
		.map(this::encoded)
		.map(this::divided)
		.map(this::subtracted)
		.value;
	}

	private String subtracted(String out) {
		return item.option(Options.SUBTRACT)
		.map(o->o.intVal)
		.filter(o->o!=0)
		.map(o->out + "+"+o)
		.orElse(out);
		
	}

	private String divided(String out) {
		return item.intOption(Options.DIVISOR)
		.filter(o->o!=0)
		.map(o->"("+out+")*"+o)
		.orElse(out);
	}
	
	private String encoded(String out) {
		return item.encoding()
		.map(e->e.unscoped + ".decode(" + out + ")")
		.orElse(out);
	}
	
	private String unprocessedDecoder() {
		if (item.scope==Scope.PACKED) { 
			return "this.getPacked(lenOrVal, ()=>this.getVarInt("+
			(item.type.builtIn.zigzag() ? "true" : "")
		+"))";
		} else {
			switch (item.type.builtIn) {
			case STRING: return "this.getString(lenOrVal)";
			case BOOL: return "!!lenOrVal";
			case MESSAGE: 
				return "this."+TypeScriptRenderer.lcFirst(item.type.message.name)+"Parser.decode("
					+ "this.buf, this.start, this.start + lenOrVal)";
			case BYTES: return "this.buf.buffer.slice(this.start, this.start + lenOrVal)";
			case INT32: case INT64: return "lenOrVal";
			case SINT32: case SINT64: return "(lenOrVal >>> 1) ^ (-(lenOrVal & 1))";
			case ENUM: case UINT32: case UINT64: return "lenOrVal"; //TODO: Bounds check for 64
			case FIXED64: return "this.getFixed(8)";//TODO: Bounds check for 64
			case FIXED32: return "this.getFixed(4)";

			default: throw new UnsupportedOperationException(""+item.type.builtIn);
			}
		}
	}

}