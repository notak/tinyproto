package me.taks.proto;

import java.util.Arrays;
import static java.util.Arrays.stream;
import java.util.Optional;
import java.util.stream.Stream;

import me.taks.proto.Field.Options;
import me.taks.proto.Field.Scope;
import me.taks.proto.ProtoEnum.Option;
import static me.taks.proto.Utils.*;

public class Message extends Type {
	public Message(Message parent, String name) {
		this(parent, name, NO_ITEMS, NO_ENUMS, NO_MSGS);
	}
	
	public Message(
		Message parent, String name, Field[] items, ProtoEnum[] enums, Message[] msgs
	) {
		this.parent = parent;
		this.name = name;
		this.items = items;
		this.enums = enums;
		this.msgs = msgs;
	}
	
	public Stream<Field> packed() {
		return stream(items).filter(i->i.scope==Scope.PACKED);
	}
	
	public Stream<Field> repeated() {
		return stream(items).filter(i->i.scope==Scope.REPEATED);
	}
	
	public static final ProtoEnum[] NO_ENUMS = new ProtoEnum[0];
	public static final Field[] NO_ITEMS = new Field[0];
	public static final Message[] NO_MSGS = new Message[0];

	public final Message parent;
	
	public final String name;
	public final ProtoEnum[] enums;
	public final Field[] items;
	public final Message[] msgs;
	public Option[] unknownOpts;

	public Message withItem(Field item) {
		return new Message(parent, name, append(items, item), enums, msgs);
	}
	
	public Message withMessage(Message msg) {
		return new Message(parent, name, items, enums, append(msgs, msg));
	}
	
	public Message withEnum(ProtoEnum e) {
		return new Message(parent, name, items, append(enums, e), msgs);
	}
	
	public Stream<Field> defaults() {
		return stream(items).filter(m->m.opts.get(Options.DEFAULT)!=null);
	}
	
	public Optional<Message> resolveDown(String typeName) {
		String[] types = typeName.split("\\.", 2);
		return typeName.length()==0 ? Optional.of(this) : 
			matching(Arrays.stream(msgs), i->i.name, types[0])
			.flatMap(m->resolveDown(types.length<2 ? "" : types[1]));
	}
	
	public Optional<Message> resolveMessage(String typeName) {
		return resolveDown(typeName).or(()->parent.resolveMessage(typeName));
	}

	public Optional<ProtoEnum> resolveEnum(String typeName) {
		int pos = typeName.lastIndexOf(".");
		String parents = pos>=0 ? typeName.substring(0, pos) : "";
		String type = typeName.substring(pos+1);
		return resolveMessage(parents)
			.flatMap(m->matching(Arrays.stream(enums), i->i.name, type));
	}
	
	public String fullName() {
		return parent.fullName() + "." + name;
	}
}