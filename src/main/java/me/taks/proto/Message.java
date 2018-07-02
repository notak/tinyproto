package me.taks.proto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.Message.Field.Scope;

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
		Field field;
		Type complex() {
			return this.field.message.resolveType(complex);
		}
		public FieldType(Field field) { this.field = field; }
	}

	public static class Field {
		
		enum Scope { REQUIRED, OPTIONAL, REPEATED, PACKED }

		public Message message;
		public String name;
		public Field.Scope scope = Scope.OPTIONAL;
		public FieldType type;
		public FieldType decodedType;
		public int number;
		public String defaultVal;
		public String encoding;
		public int divisor;
		public int subtract;
		public Map<String, String> unknownOpts = new LinkedHashMap<>();

		public FieldType decodedType() {
			return decodedType==null ? type : decodedType;
		}
	}
	
	public Message(Package pkg, Message parent, String name) {
		super(pkg, parent, name);
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