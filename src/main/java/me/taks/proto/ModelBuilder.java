package me.taks.proto;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashMap;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import me.taks.proto.Message.FieldType;
import me.taks.proto.Message.BuiltIn;
import me.taks.proto.ProtobufParser.*;
import me.taks.proto.ProtoEnum;
import me.taks.proto.ProtoEnum.Item;
import me.taks.proto.ProtoEnum.Option;

import static me.taks.proto.Field.Scope.*;

public class ModelBuilder {
	protected FieldType getType(Message m, String type) {
		FieldType out = new FieldType(m);
		try {
			out.builtIn = BuiltIn.valueOf(type.toUpperCase());
		} catch (Exception e) {
			out.builtIn = BuiltIn.COMPLEX;
			out.complex = type;
		}
		return out;
	}
	
	public Package enterProto(ProtoContext proto) {
		if (proto.pkg==null) {
			throw new Error("No package declaration");
		}
		Package p = new Package(
			proto.pkg.name.getText(),
			proto.enums.stream().map(e->protoEnum(e)).toArray(ProtoEnum[]::new),
			proto.imports.stream().map(i->i.file.getText()).toArray(String[]::new)
		);
		proto.messages.forEach(m->message(p, p, m));
		p.unknownOpts = proto.options.stream().map(this::buildOption).toArray(Option[]::new);
		if (proto.syntax!=null) p.syntax = proto.syntax.version.getText();
		//TODO  proto.imports
		return p;
	}
	
	public void messageItem(Message m, Message_itemContext c) {
		Field item = new Field(
			c.scope==null ? NONE 
			: valueOf(c.scope.getText().toUpperCase()),
			getType(m, c.type.getText()), 
			c.name.getText(), 
			parseInt(c.id.getText()));
		m.items.add(item);
		
		if (null!=c.opts) c.opts.item.forEach(rule->{
			String name = rule.getChild(0).getText();
			String value = rule.getChild(2).getText();
			if (value.startsWith("\"")) value = unescape(value);

			switch (name.toUpperCase()) {
			case "PACKED": item.scope = PACKED; break;
			case "ENCODING": item.encoding = value; break;
			case "DECODEDTYPE": item.decodedType = getType(m, value); break;
			case "DEFAULT": item.defaultVal = value; break;
			case "DIVIDE": item.divisor = parseInt(value); break;
			case "SUBTRACT": item.subtract = parseInt(value); break;
			default: 
				System.out.printf("didn't understand parameter %s parsing %s.%s", 
					name, m.name, item.name
				);
				item.unknownOpts.put(name, value);
			}
		});
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
	
	private Option buildOption(Option_lineContext o) {
		String value = o.value.getText();
		if (value.startsWith("\"")) value = unescape(value);
		return new Option(o.name.getText(), value);
	}
	
	private boolean allowAlias(Option o) {
		return "TRUE".equalsIgnoreCase(o.value) && o.is("ALLOW_ALIAS");
	}
	
	private int asInt(Token c) {
		return c==null ? 0 : parseInt(c.getText());
	}
	
	public ProtoEnum protoEnum(Enum_defContext ed) {
		String name = ed.name.getText();
		Option[] opts = 
			ed.options.stream().map(this::buildOption).toArray(Option[]::new);
		Item[] items = ed.items.stream()
			.map(i->new ProtoEnum.Item(i.name.getText(), asInt(i.value)))
			.toArray(Item[]::new);

		return new ProtoEnum(
			name, 
			stream(opts).anyMatch(this::allowAlias),
			items,
			stream(opts).filter(o->!o.is("ALLOW_ALIAS")).toArray(Option[]::new)
		);
	}

	public void message(Package p, Type owner, MessageContext ctx) {
		Message m = new Message(owner, ctx.name.getText(),
			ctx.enums.stream().map(this::protoEnum).toArray(ProtoEnum[]::new));
		owner.types.put(m.name, m);
		ctx.items.forEach(i->messageItem(m, i));
		m.unknownOpts = ctx.options.stream().map(this::buildOption).toArray(Option[]::new);
		ctx.messages.forEach(cm->message(p, m, cm));
	}

	public Package buildFile(String file) throws IOException {
		return build(CharStreams.fromFileName(file));
	}
	
	public Package build(String proto) throws IOException {
		return build(CharStreams.fromString(proto));
	}

	public Package build(CharStream cs) throws IOException {
		ProtobufLexer lexer = new ProtobufLexer(cs);
		return enterProto(new ProtobufParser(new CommonTokenStream(lexer)).proto());
	}
	
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
				Package pkg = new ModelBuilder().buildFile(protoFile);
				renderers.values().forEach(r->r.write(pkg));
			} catch (Error e) {
				System.out.println("Compilation failed: "+e.getMessage());
			}
		}
	}
}
