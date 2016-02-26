package me.taks.proto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	private Message getGlobal(List<String> names) {
		Message out = pkg.messages.get(names.remove(0));
		while (out != null && names.size()>0) {
			out = out.messages.get(names.remove(0));
		}
		return out;
	}
	
	protected Item getItem(String scope, String type, String name, String id) {
		Item i = new Item();
		try {
			i.type = Item.Type.valueOf(type.toUpperCase());
		} catch (Exception e) {
			//TODO set message
			Message base;
			String unqualifiedType;
			//is it a child?
			if (type.indexOf(".")<0) {
				base = message;
				unqualifiedType = type;
			} else {
				System.out.println("in " + type);
				List<String> parents = new ArrayList<>();
				Arrays.stream(type.split("\\.")).forEach(parents::add);
				unqualifiedType = parents.remove(parents.size()-1);
				base = getGlobal(parents);
			}
			
			if (base==null) {
				System.out.println("message or Enum "+type+" was baseless in "+message.name);
				//TODO: handle error
			} else if (base.messages.containsKey(unqualifiedType)) {
				i.type = Item.Type.MESSAGE;
				i.messageType = base.messages.get(unqualifiedType);
			} else if (base.enums.containsKey(unqualifiedType)) {
				i.type = Item.Type.ENUM;
				i.messageType = base.messages.get(unqualifiedType);
			} else {
				System.out.println("message or Enum "+type+" didn't match in "+message.name);
				//TODO: handle error
			}

			i.scope = Scope.valueOf(scope.toUpperCase());
			i.name = name;
			i.number = Integer.parseInt(id);
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
		Message.Enum e = new Message.Enum();
		e.name = ctx.getChild(1).getText();
		currentEnum = e;
		message.enums.put(e.name, e);
	}


	@Override
	public void enterEnum_item_def(Enum_item_defContext ctx) {
		currentEnum.items.put(
			ctx.getChild(0).getText(), Integer.parseInt(ctx.getChild(2).getText())
		);
	}

	@Override
	public void enterMessage_name(Message_nameContext ctx) {
		Message m = new Message(pkg, message);
		m.name = ctx.getText();
		if (message==null) pkg.messages.put(m.name, m);
		else message.messages.put(m.name, m);
		message = m;
	}

	@Override
	public void enterPackage_name(Package_nameContext ctx) {
		pkg = new Package();
		pkg.name = ctx.getText();
	}
	
	public static void main(String[] args) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(new ANTLRFileStream(args[1]));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ProtobufParser parser = new ProtobufParser(tokens);
		ProtoContext tree = parser.proto(); // parse
		ModelBuilder tsb = new ModelBuilder();
		new ParseTreeWalker().walk(tsb, tree);
		System.out.println(
			new TypeScriptRenderer().render(tsb.pkg).flatMap(o->o.lines(""))
		);
//		Files.write(
//			("\"use strict\"\n"+tsb.current.firstElement().toString()).getBytes(), 
//			new File("../nr/static/nr.proto.ts")
//		);
	}
}
