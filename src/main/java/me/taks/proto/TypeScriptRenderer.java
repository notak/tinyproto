package me.taks.proto;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.taks.proto.Message.Item;
import me.taks.proto.Message.Item.LineType;
import me.taks.proto.Message.Item.LineType.BuiltIn;
import me.taks.proto.Message.Item.Scope;

public class TypeScriptRenderer {
	private String tsType(LineType type) {
		switch (type.builtIn) {
		case BOOL: return "boolean";
		case STRING: return "string";
		case COMPLEX: return type.complex;
		default: return "number";
		}
	}
	
	public Stream<Output> renderClass(Message m) {
		Output out = new Output();
		out.head = "export class " + m.name;
		m.childEnums().map(e->
			"static " + e.name + " = {" +
			e.items.entrySet().stream().map(i->
				i.getKey() + ": " + i.getValue() + ","
			).collect(Collectors.joining(" ")) +
			"}"
		).forEach(out.lines::add);

		m.items.stream().map(i->
			i.name + " : " + 
			this.tsType(i.decodedType()) + (i.scope==Scope.REPEATED ? "[]" : "")
		).forEach(out.lines::add);

		return Stream.concat(
			m.childMessages().flatMap(this::renderClass),
			Stream.of(out)
		);
	}
	
	private String decoder(Item i) {
		Type t;
		String out = 
			//TODO: should be able to handle fixed32 and 64...
			i.scope==Scope.PACKED ? "this.getPacked(lenOrVal, this.getVarInt.bind(this))" :
			i.type.builtIn==BuiltIn.STRING ? "this.getString(lenOrVal)" :
			i.type.builtIn==BuiltIn.COMPLEX 
			&& (t = i.message.getType(i.type.complex)) instanceof Message ?
				"this."+lcFirst(t.name)+"Parser.decode("
				+ "this.buf, this.start, this.start + lenOrVal)" :
			"lenOrVal";
		
		if (i.encoding!=null) {
			out = i.encoding + ".decode(" + out + ")";
		}
		if (i.divisor>0) out += "*"+i.divisor;
		return out;
	}

	public String lcFirst(String in) {
		return Character.toLowerCase(in.charAt(0)) + in.substring(1);
	}
	
	private static final String SUBPARSER_DECL = "private %sParser: %sParser";
	private static final String SUBPARSER_INST = "this.%sParser=this.getParser(%sParser)";
	private static final String MAP_FIELD = "case %d: this._out.%s%s; break";
	
	public Stream<Output> renderParser(Message m) {
		Output out = new Output();
		out.head = "export class " + m.name + "Parser";

		out.lines(m.messages().map(i->i.name).distinct()
			.map(i->String.format(SUBPARSER_DECL, lcFirst(i), i))
		);

		out.child(
			new Output()
			.head("constructor(root: Parser<any>)")
			.line("super(root)")
			.lines(
				m.messages().map(i->i.name).distinct()
				.map(i->String.format(SUBPARSER_INST, lcFirst(i), i))
			)
		);
		
		out.child(
			new Output()
			.head("startDecode()")
			.line("var o=this._out=new " + m.name)
			.lines(m.repeated().map(i->"o." + i.name + "=[]"))
			.lines(m.packed().map(i->"o." + i.name + "=[]"))
			.lines(m.defaults().map(i->"o." + i.name + "=" + 
				(i.type.builtIn==BuiltIn.STRING ? "\"" + i.defaultVal + "\"" : i.defaultVal)
			))
		);

		out.child(
			new Output()
			.head("process(field: number, lenOrVal: number)")
			.child(new Output().head("switch (field)").lines(
				m.items.stream()
				.peek(i->System.out.println(i))
				.map(i->String.format(MAP_FIELD, i.number, i.name,
					i.scope==Scope.REPEATED ? ".push(" + decoder(i) + ")" :
					" = " + decoder(i)
				))
			))
		);
		
		return Stream.concat(
			m.childMessages().flatMap(this::renderParser),
			Stream.of(out)
		);
}

	public Stream<Output> render(Message m) {
		return Stream.concat(renderParser(m), renderClass(m));
	}
	
	public Stream<Output> render(Package p) {
		ArrayList<String> functions = new ArrayList<>();
		
		return Stream.of(
			new Output().head("module "+p.name).line("\"use strict\"")
			.lines(functions.stream())
			.children(p.childMessages().map(this::render).flatMap(x->x))
		);
	}

}
