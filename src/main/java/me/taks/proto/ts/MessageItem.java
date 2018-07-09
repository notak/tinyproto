package me.taks.proto.ts;

import me.taks.proto.Field;
import me.taks.proto.Literal;

class MessageItem {
	public static enum TSTYPE {
		BOOL, STRING, ARRAYBUFFER, NUMBER, OBJECT
	}

	public TSTYPE tsType() {
		switch (item.type.builtIn) {
		case BOOL: return TSTYPE.BOOL;
		case STRING: return TSTYPE.STRING;
		case BYTES: return TSTYPE.ARRAYBUFFER;
		case MESSAGE: return TSTYPE.OBJECT;
		default: return TSTYPE.NUMBER;
		}
	}

	public final Field item;

	public MessageItem(Field item) {
		this.item = item;
	}

	public String renderType() {
		switch (tsType()) {
		case BOOL: return "boolean";
		case STRING: return "string";
		case ARRAYBUFFER: return "ArrayBuffer";
		case OBJECT: 
			return item.type.message + (!item.repeated() ? "|undefined" : "");
		default: return "number";
		}
	}
	
	public String defaultVal() {
		return item.defaultVal().map(MessageItem::renderLiteral)
		.orElseGet(()->{
			if (item.repeated()) return "[]";
			switch (item.decodedType().builtIn) {
			case INT32: case INT64: case SINT32: case SINT64: return "0";
			case BOOL: return "false";
			case STRING: return "\"\"";
			case ENUM: return "0";
			case MESSAGE: return "undefined";
			default: return "";
			}
		});
	}

	private static String renderLiteral(Literal l) {
		switch(l.type) {
		case BOOLEAN: return l.boolVal ? "true" : "false";
		case STRING: return "\"" + l.string.replace("\"", "\\\"") + "\"";
		default: return "" + l.intVal;
		}
	}
	
}