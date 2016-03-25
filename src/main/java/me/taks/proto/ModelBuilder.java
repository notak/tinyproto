package me.taks.proto;

import java.io.IOException;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringEscapeUtils;

import me.taks.proto.Message.Field;
import me.taks.proto.Message.Field.FieldType;
import me.taks.proto.Message.Field.Scope;
import me.taks.proto.Message.Field.FieldType.BuiltIn;
import me.taks.proto.ProtobufParser.*;

public class ModelBuilder extends ProtobufBaseListener {
	private Package pkg;
	private Message message;
	private Field item;
	private Message.Enum currentEnum;

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
		i.scope = Scope.valueOf(scope.toUpperCase());
		i.name = name;
		i.number = Integer.parseInt(id);
		i.type = getType(i, type);
		return i;
	}
	
	@Override
	public void enterMessage_item_def(Message_item_defContext ctx) {
		message.items.add(item = getItem(
			ctx.getChild(0).getText(), ctx.getChild(1).getText(), 
			ctx.getChild(2).getText(), ctx.getChild(4).getText()
		));
	}
	
	@Override
	public void enterOption_line_def(Option_line_defContext ctx) {
		String name = ctx.getChild(1).getText();
		String value = ctx.getChild(3).getText();
		//TODO: bit of a hack
		if (value.startsWith("\"")) value = 
				StringEscapeUtils.unescapeJava(value.substring(1, value.length()-1));

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
			//TODO: bit of a hack
			if (value.startsWith("\"")) value = 
					StringEscapeUtils.unescapeJava(value.substring(1, value.length()-1));

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
		Message.Enum e = new Message.Enum(pkg, message, ctx.getChild(1).getText());
		currentEnum = e;
		message.types.put(e.name, e);
	}

	@Override
	public void exitEnum_def(Enum_defContext ctx) {
		currentEnum = null;
	}


	@Override
	public void enterEnum_item_def(Enum_item_defContext ctx) {
		currentEnum.items.put(
			ctx.getChild(0).getText(), Integer.parseInt(ctx.getChild(2).getText())
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
	
	public ModelBuilder buildFile(String file) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(new ANTLRFileStream(file));
		new ParseTreeWalker()
			.walk(this, new ProtobufParser(new CommonTokenStream(lexer)).proto());
		return this;
	}
	
	public ModelBuilder build(String proto) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(new ANTLRInputStream(proto));
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
				String[] parts = kv[0].substring(1).split("-");
				renderers.computeIfAbsent(parts[0], 
					i->i.equals("ts") ? new TypeScriptRenderer() : new ProtocRenderer()
				).set(parts, kv[1]);
			} else protoFile = arg;
		}
		
		if (protoFile==null || renderers.isEmpty()) {
			System.out.println("Usage: java -jar proto.jar OPTIONS <INPUT PROTO FILE>.\n"
					+ "OPTIONS:\n"
					+ "\t-ts-out=<OUTPUT FILE>.ts \n"
					+ "\t-ts-imports=<INCLUDE>.ts[:<INCLUDE>.ts]* \n"
					+ "\t-ts-includes=<INCLUDE>.ts[:<INCLUDE>.ts]* \n"
					+ "\t-proto-out=<OUTPUT FILE>.proto");
		} else {
	
			ModelBuilder mb = new ModelBuilder().buildFile(protoFile);
			renderers.values().forEach(r->r.write(mb.pkg));
		}
	}
}
