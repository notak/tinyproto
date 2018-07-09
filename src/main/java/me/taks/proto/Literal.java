package me.taks.proto;

public class Literal {
	public static enum Types {
		EMPTY, INT, STRING, BOOLEAN, TYPE
	}
	public final Types type;
	public final long intVal;
	public final String string;
	public final boolean boolVal;
	public final FieldType fieldType;
	
	public static final Literal EMPTY = 
		new Literal(Types.EMPTY, 0, null, false, null);
	public static final Literal TRUE = new Literal(true);
	public static final Literal FALSE = new Literal(false);

	private Literal(
		Types type, long intVal, String string, boolean boolVal, 
		FieldType fieldType
	) {
		this.type = type;
		this.intVal = intVal;
		this.string = string;
		this.boolVal = boolVal;
		this.fieldType = fieldType;
	}
	
	public Literal(long intVal) {
		this(Types.INT, intVal, null, false, null);
	}
	
	public Literal(String string) {
		this(Types.STRING, 0, string, false, null);
	}
	
	public Literal(boolean boolVal) {
		this(Types.BOOLEAN, 0, null, boolVal, null);
	}
	
	public Literal(FieldType type) {
		this(Types.TYPE, 0, null, false, type);
	}
}
