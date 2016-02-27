package me.taks.proto;

import me.taks.proto.Message.Enum;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Type {
	public Package pkg;
	public Message parent;
	
	public String name;
	public Map<String, Type> types = new HashMap<>();

	public Type(Package pkg, Message parent, String name) {
		super();
		this.pkg = pkg;
		this.parent = parent;
		this.name = name;
	}

	public Type getType(String typeName) {
		Type out = this;
		for (String type: typeName.split("\\.")) {
			out = out.types.get(type);
			if (out==null) break;
		}
		if (out!=null) return out;
		out = pkg;
		for (String type: typeName.split("\\.")) {
			out = out.types.get(type);
			if (out==null) break;
		}
		return out;
	}

	public Stream<Message> childMessages() {
		return types.values().stream().filter(i->i instanceof Message).map(i->(Message)i);
	}
	
	public Stream<Enum> childEnums() {
		return types.values().stream().filter(i->i instanceof Enum).map(i->(Enum)i);
	}
	
}
