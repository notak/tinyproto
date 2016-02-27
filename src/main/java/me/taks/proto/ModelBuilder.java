package me.taks.proto;

import java.io.IOException;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import me.taks.proto.Message.Item;
import me.taks.proto.Message.Item.Scope;
import me.taks.proto.ProtobufParser.*;

public class ModelBuilder extends ProtobufBaseListener {
	private Package pkg = new Package();
	private Message message;
	private Message.Enum currentEnum;
	
	protected Item getItem(String scope, String type, String name, String id) {
		Item i = new Item();
		i.scope = Scope.valueOf(scope.toUpperCase());
		i.name = name;
		i.number = Integer.parseInt(id);

		try {
			i.type = Item.Type.valueOf(type.toUpperCase());
		} catch (Exception e) {
			i.type = Item.Type.COMPLEX;
			i.complexType = type;
		}
		return i;
	}
	
	@Override
	public void enterMessage_item_def(Message_item_defContext ctx) {
		message.items.add(getItem(
			ctx.getChild(0).getText(), ctx.getChild(1).getText(), 
			ctx.getChild(2).getText(), ctx.getChild(4).getText()
		));
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
		pkg = new Package();
		pkg.name = ctx.getText();
	}
	
	public static void main(String[] args) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(new ANTLRFileStream(args[0]));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ProtobufParser parser = new ProtobufParser(tokens);
		ProtoContext tree = parser.proto(); // parse
		ModelBuilder tsb = new ModelBuilder();
		new ParseTreeWalker().walk(tsb, tree);
		System.out.println(
			new TypeScriptRenderer().render(tsb.pkg).flatMap(o->o.lines(""))
			.collect(Collectors.joining("\n"))
		);
//		Files.write(
//			("\"use strict\"\n"+tsb.current.firstElement().toString()).getBytes(), 
//			new File("../nr/static/nr.proto.ts")
//		);
	}
}
