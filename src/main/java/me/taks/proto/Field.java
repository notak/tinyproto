package me.taks.proto;

import java.util.LinkedHashMap;
import java.util.Map;

import me.taks.proto.Message.FieldType;

public class Field {
	
	enum Scope { NONE, REQUIRED, OPTIONAL, REPEATED, PACKED }

	public Field.Scope scope;
	public final FieldType type;
	public final String name;
	public final int number;
	public FieldType decodedType;
	public String defaultVal;
	public String encoding;
	public int divisor;
	public int subtract;
	public Map<String, String> unknownOpts = new LinkedHashMap<>();

	public Field(Field.Scope scope, FieldType type, String name, int number) {
		this.scope = scope;
		this.type = type;
		this.name = name;
		this.number = number;
	}

	public FieldType decodedType() {
		return decodedType==null ? type : decodedType;
	}
}