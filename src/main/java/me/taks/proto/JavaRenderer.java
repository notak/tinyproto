package me.taks.proto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.taks.proto.Message.Field;
import me.taks.proto.Message.FieldType;
import me.taks.proto.Message.FieldType.BuiltIn;
import me.taks.proto.Message.Field.Scope;

public class JavaRenderer extends Renderer {
	private String imports = "";

	public JavaRenderer set(String[] parts, String value) {
		switch (parts[1]) {
		case "imports": imports = value; break;
		default: super.set(parts, value);
		}
		return this;
	}
	
	private String javaType(FieldType type) {
		switch (type.builtIn) {
		case BOOL: return "boolean";
		case STRING: return "String";
		case BYTES: return "byte[]";
		case COMPLEX: 
			return type.complex().isEnum() ? "int" : type.complex;
		case INT32: case FIXED32: case SINT32: case SFIXED32:
			return "int";
		case INT64: case SINT64: case FIXED64: case SFIXED64:
			return "long";
		case DOUBLE:
			return "double";
		case FLOAT:
			return "float";
		case UINT32: case UINT64:
		default:
			throw new Error("uint types are not supported");
		}
	}
	
	public Stream<Output> renderClass(Message m) {
		Output out = new Output()
		.head("public class " + m.name)
		.lines(m.childEnums().map(e->
			"public static class " + e.name + " {" +
			e.items.entrySet().stream().map(i->
				"public static final int " + i.getKey() 
					+ ": " + i.getValue() + ","
			).collect(Collectors.joining(" ")) +
			"}"
		))
		.lines(m.items.stream().map(i->
			i.name + " : " + 
			javaType(i.decodedType()) + 
			(i.scope==Scope.REPEATED || i.scope==Scope.PACKED ? "[]" : "")
		));

		return Stream.concat(
			m.childMessages().flatMap(this::renderClass),
			Stream.of(out)
		);
	}
	
	private String renderLine (Field f) {
		String fn = ""; 
		String value = "r." + f.name;
		switch(f.type.builtIn) {
		case STRING:
			fn = "string"; break;
		case BOOL:
			fn = "varInt"; value += "!=0"; break;
		case INT32: case INT64:
			fn = "varInt"; break;
		case UINT32: case UINT64: //TODO: This is wrong for large values...
			fn = "varInt"; break;
		case BYTES:
			fn = "bytes"; break;
		case SINT32:
			fn = "varInt"; value += ", true"; break;
		case SINT64:
			fn = "varLong"; value += ", true"; break;
		case COMPLEX:
			Type t = f.message.resolveType(f.type.complex);
			if (t instanceof Message) {
				fn = "Builder";
				value = String.format(
					"r.%s ? new %sBuilder().build(r.%1$s) : undefined",f.name, t.name
				);
			} else fn = "VarInt"; //ENUM
			break;
		//TODO: fixed, double and byte would be handy
		default: throw new UnsupportedOperationException("" + f.type.builtIn);
		}
		if (f.scope == Scope.REPEATED) fn+="s";
		return ".set" + fn + "(" + f.number + ", " + value + ")";
	}
	
	public Stream<Output> renderBuilder(Message m) {
		Output out = new Output()
			.head("export class " + m.name + "Builder extends proto.Builder");
		Output body = new Output()
			.head("build(r: " + m.name + ")")
			.line("return this")
			.lines(m.items.stream().map(this::renderLine))
			.line(";");
		body.lineEnd = "";
		out.child(body);

		return Stream.concat(
			m.childMessages().flatMap(this::renderClass),
			Stream.of(out)
		);
	}

	private String unprocessedDecoder(Field i) {
		if (i.scope==Scope.PACKED) { 
			return "this.getPacked(lenOrVal, i=>this.getVarInt("+
			(i.type.builtIn==BuiltIn.SINT32 || i.type.builtIn == BuiltIn.SINT64 ? "true" : "")
		+"))";
		} else {
			switch (i.type.builtIn) {
			case STRING: return "this.getString(lenOrVal)";
			case BOOL: return "!!lenOrVal";
			case COMPLEX:
				Type t = i.message.resolveType(i.type.complex);
				if (t instanceof Message) {
					return "this."+lcFirst(t.name)+"Parser.decode("
					+ "this.buf, this.start, this.start + lenOrVal)";
				} else return "lenOrVal"; //ENUM
			case BYTES: return "this.buf.buffer.slice(this.start, this.start + lenOrVal)";
			case INT32: case INT64: return "lenOrVal";
			case SINT32: case SINT64: return "(lenOrVal >>> 1) ^ (-(lenOrVal & 1))";
			case UINT32: case UINT64: return "lenOrVal"; //TODO: Bounds check for 64
			case FIXED64: return "this.getFixed(8)";//TODO: Bounds check for 64
			case FIXED32: return "this.getFixed(4)";//TODO: Bounds check for 64

			default: throw new UnsupportedOperationException(""+i.type.builtIn);
			}
		}
	}
	
	private String decoder(Field i) {
		String out = unprocessedDecoder(i);
		
		if (i.encoding!=null) out = i.encoding + ".decode(" + out + ")";
		if (i.subtract!=0) out += "+"+i.subtract;
		if (i.divisor!=0) out = "("+out+")*"+i.divisor;
		return out;
	}

	public String lcFirst(String in) {
		return Character.toLowerCase(in.charAt(0)) + in.substring(1);
	}
	
	private static final String PARSER_HEAD = "export class %sParser extends Parser<%s>";
	private static final String SUBPARSER_DECL = "private %sParser: %sParser";
	private static final String SUBPARSER_INST = "this.%sParser=this.getParser(%sParser)";
	private static final String MAP_FIELD = "case %d: this._out.%s%s; break";
	
	public Stream<Output> renderParser(Message m) {
		Output out = new Output();
		out.head = String.format(PARSER_HEAD, m.name, m.name);

		out.lines(m.messages().map(i->i.name).distinct()
			.map(i->String.format(SUBPARSER_DECL, lcFirst(i), i))
		);

		out.child(
			new Output()
			.head("constructor(root?: Parser<any>)")
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
				.map(i->String.format(MAP_FIELD, i.number, i.name,
					i.scope==Scope.REPEATED ? ".add(" + decoder(i) + ")" :
					" = " + decoder(i)
				))
			))
		);
		
		return Stream.concat(
			m.childMessages().flatMap(this::renderParser),
			Stream.of(out)
		);
	}

	public Stream<Output> render(Package p) {
		return Stream.of(
			new Output().noGrouping().child(
				new Output().noGrouping()
				.line("/// <reference path=\"proto.ts\" />")
				.lines(
					Arrays.stream(imports.split(""+File.pathSeparatorChar))
					.map(s->"/// <reference path=\"" + s + "\" />")
				)
			).child(new Output().head("module "+p.name)
				.child(new Output().noGrouping()
					.line("\"use strict\"").line("import Parser=proto.Parser")
				).children(renderContent(p))
			)
		);
	}

	public void write(Package pkg) {
		super.write(pkg);
		Path proto = Paths.get(out).getParent().resolve("proto.ts");
		System.out.println("Writing file "+ proto);
		try {
			if (proto.toFile().exists()) proto.toFile().delete();
			Files.copy(this.getClass().getResourceAsStream("/proto.java"), proto);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
