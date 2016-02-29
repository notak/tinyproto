package me.taks.proto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringEscapeUtils;

import me.taks.proto.Message.Item;
import me.taks.proto.Message.Item.LineType;
import me.taks.proto.Message.Item.Scope;
import me.taks.proto.Message.Item.LineType.BuiltIn;
import me.taks.proto.ProtobufParser.*;

public class ModelBuilder extends ProtobufBaseListener {
	private Package pkg;
	private Message message;
	private Item item;
	private Message.Enum currentEnum;

	protected LineType getType(String type) {
		LineType out = new LineType();
		try {
			out.builtIn = BuiltIn.valueOf(type.toUpperCase());
		} catch (Exception e) {
			out.builtIn = BuiltIn.COMPLEX;
			out.complex = type;
		}
		return out;
		
	}
	
	protected Item getItem(String scope, String type, String name, String id) {
		Item i = new Item();
		i.message = message;
		i.scope = Scope.valueOf(scope.toUpperCase());
		i.name = name;
		i.number = Integer.parseInt(id);
		i.type = getType(type);
		return i;
	}
	
	@Override
	public void enterMessage_item_def(Message_item_defContext ctx) {
		System.out.println(ctx.getChild(5));
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
			case "DECODEDTYPE": item.decodedType = getType(value); break;
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
	
	public static void main(String[] args) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(new ANTLRFileStream(args[0]));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ProtobufParser parser = new ProtobufParser(tokens);
		ProtoContext tree = parser.proto(); // parse
		ModelBuilder tsb = new ModelBuilder();
		new ParseTreeWalker().walk(tsb, tree);
		Files.write(Paths.get("/home/chris/workspace/proto/tmp.proto"),
//				new TypeScriptRenderer().render(tsb.pkg).flatMap(o->o.lines("    "))
//				.collect(Collectors.joining("\n"))
				new ProtocRenderer().render(tsb.pkg).flatMap(o->o.lines("\t"))
				.collect(Collectors.joining("\n")).getBytes()
		);
//		Files.write(
//			("\"use strict\"\n"+tsb.current.firstElement().toString()).getBytes(), 
//			new File("../nr/static/nr.proto.ts")
//		);
	}
}
