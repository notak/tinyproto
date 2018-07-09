package me.taks.proto;

import java.util.stream.Stream;
import static java.util.Arrays.stream;
import me.taks.proto.Field.Scope;

public class ProtocRenderer extends Renderer {
	
	private Output renderEnum(ProtoEnum e) {
		return new Output().head("enum " + e.name)
		.line(e.allowAlias ? "option allow_alias = true" : null)
		.lines(stream(e.unknownOpts).map(i->
			"option " + i.name + " = \"" + i.value + "\""
		))
		.lines(stream(e.items).map(i->i.name + " = " + i.value));
	}
	
	private String renderType(FieldType type) {
		switch(type.builtIn) {
		case MESSAGE: return type.message.fullName();
		case ENUM: return type.protoEnum.fullName();
		default: return type.builtIn.name().toLowerCase();
		}
	}
	
	private String renderItem(Package pkg, Field i) {
		return renderScope(pkg.syntax, i.scope)
		+ renderType(i.type) + " " + i.name + " = "
		+ i.number + (i.scope==Scope.PACKED ? " [packed=true]" : "");
	}

	@Override
	public Stream<Output> renderClass(Package pkg, Message m) {
		Output out = new Output().head("message " + m.name);
		out.children(stream(m.enums).map(this::renderEnum));
		out.lines(stream(m.items).map(i->renderItem(pkg, i)));
		out.lines(
			stream(m.unknownOpts).map(i->"option " + i.name + " = " + i.value)
		);
		
		out.children(stream(m.msgs).flatMap(cm->renderClass(pkg, cm)));

		return Stream.of(out);
	}
	
	public Stream<Output> render(Package p) {
		Output out = new Output().head("package "+p.name);
		if (p.syntax.length()>0) out.head("syntax=" + p.syntax);
		out.emptyBody = ";";
		return Stream.of(
			Stream.of(out),
			stream(p.imports).map(n->"import " + n)
				.map(new Output().emptyBody(";")::head),
			stream(p.unknownOpts).map(i->
				"option " + i.name + " = \"" + i.value + "\""
			).map(new Output().emptyBody(";")::head),
			renderContent(p)
		).flatMap(i->i);
	}
}
