package me.taks.proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import me.taks.proto.Message.Item.Scope;
import me.taks.proto.Message.Item.Type;

public class Message {
	
	public Message(Package pkg, Message parent) {
		super();
		this.pkg = pkg;
		this.parent = parent;
	}

	public static class Item {
		enum Type {
			INT32, SINT32, UINT32, INT64, SINT64, UINT64,
			BOOL, 
			STRING,
			MESSAGE,
			ENUM,
		}
		
		enum Scope {
			REQUIRED, OPTIONAL, REPEATED
		}

		public String name;
		public Item.Scope scope;
		public Item.Type type;
		public int number;
		public Message messageType;
		public Enum enumType;
	}

	public static class Enum {
		public Message parent;
		public String name;
		public Map<String, Integer> items = new HashMap<>();
	}
	
	public Package pkg;
	public Message parent;
	
	public String name;
	public List<Item> items = new ArrayList<>();

	public Map<String, Message> messages = new HashMap<>();
	public Map<String, Enum> enums = new HashMap<>();
	
	public Stream<Item> children() {
		return items.stream().filter(i->i.scope==Scope.REPEATED);
	}
	
	public Stream<Item> repeated() {
		return items.stream().filter(i->i.type==Type.MESSAGE);
	}
}