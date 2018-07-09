package me.taks.proto.ts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.stream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.taks.proto.Field;
import me.taks.proto.Message;
import me.taks.proto.Output;
import me.taks.proto.Package;
import me.taks.proto.Renderer;
import me.taks.proto.Field.Scope;

public class TypeScriptRenderer extends Renderer {
	private String imports = "";
	
	public TypeScriptRenderer set(String[] parts, String value) {
		switch (parts[1]) {
		case "imports": imports = value; break;
		default: super.set(parts, value);
		}
		return this;
	}
	
	public Stream<Output> renderClass(Message m) {
		Output out = new Output()
		.head("export class " + m.name)
		.lines(stream(m.enums).map(e->
			"static " + e.name + " = {" +
			Arrays.stream(e.items).map(i->i.name + ": " + i.value + ","
			).collect(Collectors.joining(" ")) +
			"}"
		))
		.lines(stream(m.items)
			.map(MessageItem::new)
			.map(i->
				i.item.name + " : " + 
				i.renderType() + 
				(i.item.repeated() ? "[]" : "") +
				(i.defaultVal().length()>0 ? (" = " + i.defaultVal()) : "")
		));

		return Stream.concat(
			stream(m.msgs).flatMap(this::renderClass),
			Stream.of(out)
		);
	}
	
	private String getFn (Message m, Field f) {
		switch(f.type.builtIn) {
		case STRING: return "String";

		case BOOL: case INT32: case INT64: case UINT32: case UINT64: 
		case SINT32: case SINT64: case ENUM:
			return "VarInt";

		case BYTES: return "ArrayBuffer";

		case MESSAGE: return "Builder";

		//TODO: fixed, double and byte would be handy
		default: throw new UnsupportedOperationException("" + f.type.builtIn);
		}
	}
	
	private String getVal(Message m, Field f) {
		switch(f.type.builtIn) {
		case BOOL: return "r." + f.name + " ? 1 : undefined";
		
		case SINT32: case SINT64: return "r." + f.name + ", true";
		
		case MESSAGE: return String.format(
			f.scope == Scope.REPEATED 
				? "r.%s.map(x=>new %sBuilder().build(x))"
				: "r.%s ? new %sBuilder().build(r.%1$s) : undefined",
			f.name, f.type.message.name
		);

		default: return "r." + f.name;
		}
	}
	
	private String renderLine (Message m, Field f) {
		String fn = getFn(m, f) + (f.repeated() ? "s" : "");
		return ".set" + fn + "(" + f.number + ", " + getVal(m, f) + ")";
	}
	
	public Stream<Output> renderBuilder(Message m) {
		Output out = new Output()
			.head("export class " + m.name + "Builder extends Builder");
		Output body = new Output()
			.head("build(r: " + m.name + ")")
			.line("return this")
			.lines(stream(m.items).map(f->renderLine(m, f)))
			.line(";");
		body.lineEnd = "";
		out.child(body);

		return Stream.concat(
			stream(m.msgs).flatMap(this::renderClass),
			Stream.of(out)
		);
	}

	public static class Chain<T> {
		public static<T> Chain<T> chain(T t) {
			return new Chain<>(t);
		}
		public Chain(T t) {
			this.value = t;
		}
		public final T value;
		public<U> Chain<U> map(Function<T, U> map) {
			return chain(map.apply(value));
		}
		public<V, W> Chain<V> map(BiFunction<T, W, V> map, W w) {
			return chain(map.apply(value, w));
		}
	}
	
	public static String lcFirst(String in) {
		return Character.toLowerCase(in.charAt(0)) + in.substring(1);
	}
	
	public Stream<Output> render(Package p) {
		return Stream.of(
			new Output().noGrouping().child(
				new Output().noGrouping()
				.line("import { Parser, Builder } from \"./proto.js\"")
				.lines(
					Arrays.stream(imports.split(""+File.pathSeparatorChar))
					.map(s->"import * as "+s+"  from \"./" + s + ".js\"")
				)
			).children(renderContent(p))
		);
	}

	public void write(Package pkg) {
		super.write(pkg);
		Path proto = Paths.get(out).getParent().resolve("proto.ts");
		System.out.println("Writing file "+ proto);
		try {
			if (proto.toFile().exists()) proto.toFile().delete();
			Files.copy(this.getClass().getResourceAsStream("/proto.ts"), proto);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
