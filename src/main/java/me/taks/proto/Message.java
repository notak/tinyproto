package me.taks.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import me.taks.proto.Field.Scope;

public class Message extends Type {
	enum BuiltIn {
		INT32, SINT32, UINT32, INT64, SINT64, UINT64,
		FIXED32, FIXED64, SFIXED32, SFIXED64,
		DOUBLE, FLOAT,
		BOOL, 
		STRING,
		BYTES,
		COMPLEX,
	}

	public static class FieldType {
		BuiltIn builtIn;
		String complex;
		Message m;
		Type complex() {
			return this.m.resolveType(complex);
		}
		public FieldType(Message m) { this.m = m; }
	}

	public Message(Type parent, String name, ProtoEnum[] enums) {
		super(parent, name, enums);
	}
	
	public List<Field> items = new ArrayList<>();
	
	public Stream<Field> complex() {
		return items.stream()
			.filter(i->i.type.builtIn==BuiltIn.COMPLEX);
	}
	
	public Stream<Type> messages() {
		return complex()
			.map(i->resolveType(i.type.complex))
			.filter(i->i instanceof Message);
	}
	
	public Stream<Field> packed() {
		return items.stream().filter(i->i.scope==Scope.PACKED);
	}
	
	public Stream<Field> repeated() {
		return items.stream().filter(i->i.scope==Scope.REPEATED);
	}

	public Stream<Field> defaults() {
		return items.stream().filter(i->i.defaultVal!=null);
	}
}