package me.taks.proto;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashMap;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import me.taks.proto.Message.Field;
import me.taks.proto.Message.FieldType;
import me.taks.proto.Message.Field.Scope;
import me.taks.proto.Message.BuiltIn;
import me.taks.proto.ProtobufParser.*;
import me.taks.proto.Type.ProtoEnum;

public class ModelBuilder extends ProtobufBaseListener {
	private Package pkg;
	private Message message;
	private Field item;
	private Message.ProtoEnum currentEnum;

	protected FieldType getType(Field field, String type) {
		FieldType out = new FieldType(field);
		try {
			out.builtIn = BuiltIn.valueOf(type.toUpperCase());
		} catch (Exception e) {
			out.builtIn = BuiltIn.COMPLEX;
			out.complex = type;
		}
		return out;
	}
	
	protected Field getItem(String scope, String type, String name, String id) {
		Field i = new Field();
		i.message = message;
		i.scope = scope==null ? Scope.OPTIONAL : Scope.valueOf(scope.toUpperCase());
		i.name = name;
		i.number = Integer.parseInt(id);
		i.type = getType(i, type);
		return i;
	}
	
	@Override
	public void enterImport_file_name(Import_file_nameContext ctx) {
		pkg.imports.add(ctx.getText());
	}

	private String childText(ParserRuleContext ctx, int pos) {
		return ctx.getChild(pos).getText();
	}
	
	@Override
	public void enterMessage_item_def(Message_item_defContext ctx) {
		message.items.add(item = getItem(
			ctx.PROTOBUF_SCOPE_LITERAL()==null ? null : ctx.PROTOBUF_SCOPE_LITERAL().getText(),
			ctx.proto_type().getText(),
			ctx.IDENTIFIER().getText(),
			ctx.INTEGER_LITERAL().getText()
		));
	}
	
	//TODO: bit of a hack but who wants a commons dependency
	private String unescape(String in) {
		StreamTokenizer parser = new StreamTokenizer(new StringReader(in));
		try {
		  parser.nextToken();
		  if (parser.ttype == '"') return parser.sval;
		  else throw new Error("ERROR!");
		}
		catch (IOException e) {
		  throw new Error(e);
		}		
	}
	
	@Override
	public void enterOption_line_def(Option_line_defContext ctx) {
		String name = childText(ctx, 1);
		String value = childText(ctx, 3);
		if (value.startsWith("\"")) value = unescape(value);

		if (currentEnum!=null) { //enum options
			switch (name.toUpperCase()) {
			case "ALLOW_ALIAS": currentEnum.allowAlias =true; break;
			default: currentEnum.unknownOpts.put(name, value);
			}
		} else if (message!=null) { //message options
			message.unknownOpts.put(name, value);
		} else { //package options
			pkg.unknownOpts.put(name, value);
		}
	}
	
	@Override
	public void enterOption_field_def(Option_field_defContext ctx) {
		for (int i=1; i<ctx.getChildCount()-1; i+=2) {
			Option_field_itemContext rule = 
					(Option_field_itemContext)ctx.getChild(i).getPayload();
			String name = rule.getChild(0).getText();
			String value = rule.getChild(2).getText();
			if (value.startsWith("\"")) value = unescape(value);

			switch (name.toUpperCase()) {
			case "PACKED": item.scope = Scope.PACKED; break;
			case "ENCODING": item.encoding = value; break;
			case "DECODEDTYPE": item.decodedType = getType(item, value); break;
			case "DEFAULT": item.defaultVal = value; break;
			case "DIVIDE": item.divisor = Integer.parseInt(value); break;
			case "SUBTRACT": item.subtract = Integer.parseInt(value); break;
			default: 
				System.out.printf("didn't understand parameter %s parsing %s.%s", 
					name, message.name, item.name
				);
				item.unknownOpts.put(name, value);
			}
		}
	}
	
	@Override
	public void exitMessage_def(Message_defContext ctx) {
		message = message.parent;
	}
	
	@Override
	public void enterEnum_def(Enum_defContext ctx) {
		String name = ctx.getChild(1).getText();
		if (message==null) {
			throw new Error(String.format("enum %s has no enclosing message", name));
		}
		ProtoEnum e = currentEnum = new ProtoEnum(pkg, message, name);
		message.types.put(e.name, e);
	}

	@Override
	public void exitEnum_def(Enum_defContext ctx) {
		currentEnum = null;
	}


	@Override
	public void enterEnum_item_def(Enum_item_defContext ctx) {
		currentEnum.items.put(
				childText(ctx, 0), Integer.parseInt(childText(ctx, 2))
		);
	}

	@Override
	public void enterMessage_name(Message_nameContext ctx) {
		Message m = new Message(pkg, message, ctx.getText());
		if (message==null) pkg.types.put(m.name, m);
		else message.types.put(m.name, m);
		message = m;
	}

	@Override
	public void enterPackage_name(Package_nameContext ctx) {
		pkg = new Package(ctx.getText());
	}
	
	@Override
	public void enterSyntax_line_def(Syntax_line_defContext ctx) {
		pkg.syntax = childText(ctx, 2);
	}
	
	public ModelBuilder buildFile(String file) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(CharStreams.fromFileName(file));
		new ParseTreeWalker()
			.walk(this, new ProtobufParser(new CommonTokenStream(lexer)).proto());
		return this;
	}
	
	public ModelBuilder build(String proto) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(CharStreams.fromString(proto));
		new ParseTreeWalker()
			.walk(this, new ProtobufParser(new CommonTokenStream(lexer)).proto());
		return this;
	}
	
	public Package pkg() { return this.pkg; }
	
	public static void main(String[] args) throws IOException {
		HashMap<String, Renderer> renderers = new HashMap<>();
		String protoFile = null;
		
		for (String arg: args) {
			if (arg.contains("=")) {
				String[] kv = arg.split("=");
				String v = kv.length>1 ? kv[1] : "";
				String[] parts = kv[0].substring(1).split("-");
				renderers.computeIfAbsent(parts[0], 
					i->i.equals("ts") ? new TypeScriptRenderer() : new ProtocRenderer()
				).set(parts, v);
			} else protoFile = arg;
		}
		
		if (protoFile==null || renderers.isEmpty()) {
			System.out.println("Usage: java -jar proto.jar OPTIONS <INPUT PROTO FILE>.\n"
					+ "OPTIONS:\n"
					+ "\t-[ts|proto]-include-parsers=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-[ts|proto]-exclude-parsers=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-[ts|proto]-include-classes=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-[ts|proto]-exclude-classes=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-[ts|proto]-include-builders=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-[ts|proto]-exclude-builders=<MESSAGE>[,<MESSAGE>]* \n"
					+ "\t-ts-out=<OUTPUT FILE>.ts \n"
					+ "\t-ts-imports=<INCLUDE>.ts[:<INCLUDE>.ts]* \n"
					+ "\t-ts-includes=<INCLUDE>.ts[:<INCLUDE>.ts]* \n"
					+ "\t-proto-out=<OUTPUT FILE>.proto");
		} else {
			try {
				ModelBuilder mb = new ModelBuilder().buildFile(protoFile);
				renderers.values().forEach(r->r.write(mb.pkg));
			} catch (Error e) {
				System.out.println("Compilation failed: "+e.getMessage());
			}
		}
	}
}
