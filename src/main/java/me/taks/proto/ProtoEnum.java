package me.taks.proto;

public class ProtoEnum {
	public static class Option {
		public final String name;
		public final String value;
		public Option(String name, String value) {
			this.name = name;
			this.value = value;
		}
		public boolean is(String name) {
			return name.equalsIgnoreCase(this.name);
		}
	}
	
	public static class Item {
		public final String name;
		public final int value;
		public Item(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}

	public final Message parent;
	public final String name;
	public final boolean allowAlias;
	public final Item[] items;
	public final Option[] unknownOpts;

	public ProtoEnum(
		Message parent, String name, boolean allowAlias, 
		Item[] items, Option[] unknownOpts
	) {
		this.parent = parent;
		this.name = name;
		this.allowAlias = allowAlias;
		this.items = items;
		this.unknownOpts = unknownOpts;
	}

	public String fullName() {
		return parent.fullName() + "." + name;
	}
}