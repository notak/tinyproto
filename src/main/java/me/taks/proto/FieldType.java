package me.taks.proto;

public class FieldType {
	public enum BuiltIn {
		INT32, SINT32, UINT32, INT64, SINT64, UINT64,
		FIXED32, FIXED64, SFIXED32, SFIXED64,
		DOUBLE, FLOAT,
		BOOL, 
		STRING,
		BYTES,
		ENUM,
		MESSAGE,
		UNSCOPED;

		public boolean zigzag() {
			return this==SINT32 || this==SINT64;
		}
	}

	public final BuiltIn builtIn;
	public final ProtoEnum protoEnum;
	public final Message message;
	public final String unscoped;


	private FieldType(
		BuiltIn builtIn, ProtoEnum protoEnum, Message message, String unscoped
	) {
		this.builtIn = builtIn;
		this.protoEnum = protoEnum;
		this.message = message;
		this.unscoped = unscoped;
	}

	public FieldType(Message m) { 
		this(BuiltIn.MESSAGE, null, m, null);
	}

	public FieldType(BuiltIn b) { 
		this(b, null, null, null);
	}

	public FieldType(ProtoEnum pe) { 
		this(BuiltIn.ENUM, pe, null, null);
	}

	public FieldType(String unscoped) { 
		this(BuiltIn.UNSCOPED, null, null, unscoped);
	}
}