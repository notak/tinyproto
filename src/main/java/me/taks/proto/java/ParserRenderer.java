package me.taks.proto.java;

import static java.util.Arrays.stream;

import java.util.stream.Stream;

import me.taks.proto.Message;
import me.taks.proto.Output;
import me.taks.proto.Field.Scope;

public class ParserRenderer {
	public final Message msg;

	public ParserRenderer(Message msg) {
		this.msg = msg;
	}
	
	static final String PARSER_HEAD = 
			"export class %sParser extends Parser<%s>";
	static final String SUBPARSER_DECL = 
			"protected %sParser: %sParser";
	static final String SUBPARSER_INST = 
			"this.%sParser=new %sParser()";
	static final String MAP_FIELD = 
			"case %d: this._out.%s%s; break";
	
	public Stream<Output> render() {
		Output out = new Output();
		out.head = String.format(PARSER_HEAD, msg.name, msg.name);

		out.lines(stream(msg.msgs).map(i->i.name).distinct()
			.map(i->String.format(SUBPARSER_DECL, JavaRenderer.lcFirst(i), i))
		);

		out.child(
			new Output()
			.head("constructor()")
			.line("super()")
			.lines(
				stream(msg.msgs).map(i->i.name).distinct()
				.map(i->String.format(SUBPARSER_INST, JavaRenderer.lcFirst(i), i, i))
			)
		);
		
		out.child(
			new Output()
			.head("startDecode()")
			.line("const o=this._out=new " + msg.name)
			.lines(msg.repeated().map(i->"o." + i.name + "=[]"))
			.lines(msg.packed().map(i->"o." + i.name + "=[]"))
			.lines(msg.defaults().map(i->"o." + i.name + "=" + i.defaultVal()))
		);

		out.child(
			new Output()
			.head("process(field: number, lenOrVal: number)")
			.child(new Output().head("switch (field)").lines(
				stream(msg.items)
				.map(MessageItemDecoder::new)
				.map(i->String.format(MAP_FIELD, i.item.number, i.item.name,
					i.item.scope==Scope.REPEATED 
						? ".push(" + i.decoder() + ")" 
						: " = " + i.decoder()
				))
			))
		);
		
		return Stream.concat(
			stream(msg.msgs).flatMap(ParserRenderer::render),
			Stream.of(out)
		);
	}

	public static Stream<Output> render(Message m) {
		return new ParserRenderer(m).render();
	}
}