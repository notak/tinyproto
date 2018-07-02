package me.taks.proto;

import java.util.stream.Stream;

import me.taks.proto.Message.Field.Scope;

public class ProtocRenderer extends Renderer {
	@Override
	public Stream<Output> renderClass(Message m) {
		Output out = new Output().head("message " + m.name)
		.children(m.childEnums().map(e->
			new Output().head("enum " + e.name)
				.line(e.allowAlias ? "option allow_alias = true" : null)
				.lines(e.unknownOpts.entrySet().stream().map(i->
					"option " + i.getKey() + " = \"" + i.getValue() + "\""
				))
				.lines(e.items.entrySet().stream().map(i->
					i.getKey() + " = " + i.getValue()
				)
			))
		).lines(
			m.items.stream().map(i->
				renderScope(m.pkg.syntax, i.scope)
				+ " " +
				(i.type.complex!=null ? i.type.complex : i.type.builtIn.toString().toLowerCase()) 
				+ " " + i.name + " = "
				+ i.number + (i.scope==Scope.PACKED ? " [packed=true]" : "")
			)
		).lines(m.unknownOpts.entrySet().stream().map(i->
			"option " + i.getKey() + " = " + i.getValue()
		));
		
		out.children(m.childMessages().flatMap(this::renderClass));

		return Stream.of(out);
	}
	
	private String renderScope(String syntax, Scope scope) {
		if (syntax.length()>0 && scope==Scope.OPTIONAL) return "";
		return scope==Scope.PACKED ? "repeated" : scope.toString().toLowerCase();
	}
	
	public Stream<Output> render(Package p) {
		Output out = new Output().head("package "+p.name);
		if (p.syntax.length()>0) out.head("syntax=" + p.syntax);
		out.emptyBody = ";";
		return Stream.of(
			Stream.of(out),
			p.imports.stream().map(n->"import " + n)
				.map(new Output().emptyBody(";")::head),
			p.unknownOpts.entrySet().stream().map(i->
				"option " + i.getKey() + " = \"" + i.getValue() + "\""
			).map(new Output().emptyBody(";")::head),
			renderContent(p)
		).flatMap(i->i);
	}
}
