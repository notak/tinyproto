package me.taks.proto;

import static java.util.Arrays.stream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.ProtoEnum.Option;

public class Type {
	public final Type parent;
	
	public final String name;
	public final Map<String, Type> types = new LinkedHashMap<>();
	public final ProtoEnum[] enums;
	public Option[] unknownOpts;

	public Type(Type parent, String name, ProtoEnum[] enums) {
		this.parent = parent;
		this.name = name;
		this.enums = enums;
	}

	public Type resolveType(String typeName) {
		Type out = this;
		for (String type: typeName.split("\\.")) {
			out = out.types.get(type);
			if (out==null) break;
		}
		if (parent!=null) return parent.resolveType(typeName);
		return null;
	}

	public Stream<Message> childMessages() {
		return types.values().stream()
			.filter(i->i instanceof Message)
			.map(i->(Message)i);
	}
	
	public Stream<ProtoEnum> childEnums() {
		return stream(enums);
	}
	
	public boolean isEnum() { return false; }
}
