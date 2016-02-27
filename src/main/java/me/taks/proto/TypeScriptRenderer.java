package me.taks.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.taks.proto.Message.Item;
import me.taks.proto.Message.Item.Scope;

public class TypeScriptRenderer {
	public static class Output {
		public String head;
		public String lineEnd = ";";
		public String startBrace = "{";
		public String endBrace = "}";
		public List<Output> children = new ArrayList<>();
		public List<String> lines = new ArrayList<>();
		
		public Output head(String head) {
			this.head = head;
			return this;
		}
		
		public Output line(String line) {
			this.lines.add(line);
			return this;
		}
		
		public Output lines(Stream<String> lines) {
			lines.forEach(this.lines::add);
			return this;
		}
		
		public Output child(Output child) {
			this.children.add(child);
			return this;
		}
		
		public Output children(Stream<Output> children) {
			children.forEach(this.children::add);
			return this;
		}
		
		public Stream<String> lines(String indent) {
			return Stream.of(
				Stream.of(head + " " + startBrace), 
				children.stream().flatMap(c->c.lines(indent)).map(s->indent + s),
				lines.stream().map(l->indent + l + lineEnd),	
				Stream.of(endBrace)
			).flatMap(x->x);
		}
	}

	private String tsType(Item item) {
		switch (item.type) {
		case INT32: case SINT32: case UINT32: case INT64: case SINT64: case UINT64:
			return "number";
		case BOOL: return "boolean";
		case STRING: return "string";
		case COMPLEX: return item.complexType;
		default: return null;
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
			this.tsType(i) + (i.scope==Scope.REPEATED ? "[]" : "")
		).forEach(out.lines::add);

		return Stream.concat(
			m.childMessages().flatMap(this::renderClass),
			Stream.of(out)
		);
	}
	
	private String decoder(Item i) {
		switch (i.type) {
		case STRING:
			return "this.getString(lenOrVal)";
			//todo: message types and booleans
		default: return "lenOrVal";
		}
	}

	public String lcFirst(String in) {
		return Character.toLowerCase(in.charAt(0)) + in.substring(1);
	}
	
	private static final String SUBPARSER_DECL = "private %sParser: %sParser";
	private static final String SUBPARSER_INST = "this.%sParser=this.getParser(%sParser)";
	private static final String MAP_FIELD = "case %d: this._out.%s = %s; break;";
	
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
				m.messages().map(i->i.name + "Parser").distinct()
				.map(i->String.format(SUBPARSER_INST, lcFirst(i), i))
			)
		);

		out.child(
			new Output()
			.head("startDecode()")
			.line("this._out = new " + m.name)
			.lines(m.repeated().map(i->"this._out."+i.name+" = []"))
		);

		out.child(
			new Output()
			.head("process(field: number, lenOrVal: number)")
			.child(new Output().head("switch (field)").lines(
				m.items.stream()
				.peek(i->System.out.println(i))
				.map(i->String.format(MAP_FIELD, i.number, i.name,
					i.scope==Scope.REPEATED ? ".push(" + decoder(i) + ")" : decoder(i)
				))
			))
		);
		
		m.items.stream().map(i->
			i.name + " : " + 
			this.tsType(i) + (i.scope==Scope.REPEATED ? "[]" : "")
		).forEach(out.lines::add);

		return Stream.concat(
			m.childMessages().flatMap(this::renderParser),
			Stream.of(out)
		);
}

	public Stream<Output> render(Message m) {
		return Stream.concat(renderParser(m), renderClass(m));
	}
	
	public Stream<Output> render(Package p) {
		return Stream.of(new Output().head("module "+p.name).children(
			p.childMessages().map(this::render).flatMap(x->x)
		));
	}

}
