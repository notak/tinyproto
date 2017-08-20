package me.taks.proto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Type {
	public final Package pkg;
	public final Message parent;
	
	public final String name;
	public final Map<String, Type> types = new LinkedHashMap<>();
	public final Map<String, String> unknownOpts = new LinkedHashMap<>();

	public Type(Package pkg, Message parent, String name) {
		this.pkg = pkg;
		this.parent = parent;
		this.name = name;
	}

	public Type resolveType(String typeName) {
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
		return types.values().stream()
			.filter(i->i instanceof Message)
			.map(i->(Message)i);
	}
	
	public Stream<Enum> childEnums() {
		return types.values().stream()
			.filter(i->i instanceof Enum)
			.map(i->(Enum)i);
	}
	
	public boolean isEnum() { return false; }

	public static class Enum extends Type {
		public Enum(Package pkg, Message parent, String name) {
			super(pkg, parent, name);
			// TODO Auto-generated constructor stub
		}
		public final Map<String, Integer> items = new LinkedHashMap<>();
		public final Map<String, String> unknownOpts = new LinkedHashMap<>();
		public boolean allowAlias;
		public boolean isEnum() { return true; }
	}
}
