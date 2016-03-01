package me.taks.proto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.Message.Field.Scope;
import me.taks.proto.Message.Field.FieldType.BuiltIn;

public class Message extends Type {
	
	public Message(Package pkg, Message parent, String name) {
		super(pkg, parent, name);
	}

	public static class Field {
		public static class FieldType {
			enum BuiltIn {
				INT32, SINT32, UINT32, INT64, SINT64, UINT64,
				BOOL, 
				STRING,
				COMPLEX,
			}
			BuiltIn builtIn;
			String complex;
			Field field;
			Type complex() {
				return this.field.message.getType(complex);
			}
			public FieldType(Field field) { this.field = field; }
		}
		
		enum Scope {
			REQUIRED, OPTIONAL, REPEATED, PACKED
		}

		public Message message;
		public String name;
		public Field.Scope scope;
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
	
	public static class Enum extends Type {
		public Enum(Package pkg, Message parent, String name) {
			super(pkg, parent, name);
			// TODO Auto-generated constructor stub
		}
		public Map<String, Integer> items = new LinkedHashMap<>();
		public Map<String, String> unknownOpts = new LinkedHashMap<>();
		public boolean allowAlias;
	}
	
	public List<Field> items = new ArrayList<>();
	
	public Stream<Field> complex() {
		return items.stream().filter(i->i.type.builtIn==BuiltIn.COMPLEX);
	}
	
	public Stream<Type> messages() {
		return complex().map(i->getType(i.type.complex)).filter(i->i instanceof Message);
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