package me.taks.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.Message.Item.Scope;
import me.taks.proto.Message.Item.LineType.BuiltIn;

public class Message extends Type {
	
	public Message(Package pkg, Message parent, String name) {
		super(pkg, parent, name);
	}

	public static class Item {
		public static class LineType {
			enum BuiltIn {
				INT32, SINT32, UINT32, INT64, SINT64, UINT64,
				BOOL, 
				STRING,
				COMPLEX,
			}
			BuiltIn builtIn;
			String complex;
		}
		
		enum Scope {
			REQUIRED, OPTIONAL, REPEATED, PACKED
		}

		public Message message;
		public String name;
		public Item.Scope scope;
		public LineType type;
		public LineType decodedType;
		public int number;
		public String defaultVal;
		public String encoding;
		public int divisor;

		public LineType decodedType() {
			return decodedType==null ? type : decodedType;
		}
	}
	
	public static class Enum extends Type {
		public Enum(Package pkg, Message parent, String name) {
			super(pkg, parent, name);
			// TODO Auto-generated constructor stub
		}
		public Map<String, Integer> items = new HashMap<>();
	}
	
	public List<Item> items = new ArrayList<>();
	
	public Stream<Item> complex() {
		return items.stream().filter(i->i.type.builtIn==BuiltIn.COMPLEX);
	}
	
	public Stream<Type> messages() {
		return complex().map(i->getType(i.type.complex)).filter(i->i instanceof Message);
	}
	
	public Stream<Item> packed() {
		return items.stream().filter(i->i.scope==Scope.PACKED);
	}
	
	public Stream<Item> repeated() {
		return items.stream().filter(i->i.scope==Scope.REPEATED);
	}

	public Stream<Item> defaults() {
		return items.stream().filter(i->i.defaultVal!=null);
	}
}