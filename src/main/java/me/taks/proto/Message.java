package me.taks.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.Message.Item.Scope;

public class Message extends Type {
	
	public Message(Package pkg, Message parent, String name) {
		super(pkg, parent, name);
	}

	public static class Item {
		enum Type {
			INT32, SINT32, UINT32, INT64, SINT64, UINT64,
			BOOL, 
			STRING,
			COMPLEX,
		}
		
		enum Scope {
			REQUIRED, OPTIONAL, REPEATED
		}

		public String name;
		public Item.Scope scope;
		public Item.Type type;
		public int number;
		public String complexType;
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
		return items.stream().filter(i->i.type==Item.Type.COMPLEX);
	}
	
	public Stream<Type> messages() {
		return complex().map(i->getType(i.complexType)).filter(i->i instanceof Message);
	}
	
	public Stream<Item> repeated() {
		return items.stream().filter(i->i.scope==Scope.REPEATED);
	}
}