package me.taks.proto.java;

import me.taks.proto.Field;
import me.taks.proto.Literal;

class MessageItem {
	public static enum TSTYPE {
		BOOL, STRING, BYTEARRAY, INT, LONG, DOUBLE, FLOAT, OBJECT
	}

	public TSTYPE tsType() {
		switch (item.type.builtIn) {
		case BOOL: return TSTYPE.BOOL;
		case STRING: return TSTYPE.STRING;
		case BYTES: return TSTYPE.BYTEARRAY;
		case MESSAGE: return TSTYPE.OBJECT;
		case ENUM: case INT32: case FIXED32: case SINT32: case SFIXED32:
			return TSTYPE.INT;
		case INT64: case SINT64: case FIXED64: case SFIXED64:
			return TSTYPE.LONG;
		case DOUBLE:
			return TSTYPE.DOUBLE;
		case FLOAT:
			return TSTYPE.FLOAT;
		case UINT32: case UINT64:
		default:
			throw new Error("uint types are not supported");
		}
	}

	public final Field item;

	public MessageItem(Field item) {
		this.item = item;
	}

	public String renderType() {
		switch (tsType()) {
		case BOOL: return "boolean";
		case STRING: return "String";
		case BYTEARRAY: return "byte[]";
		case OBJECT: return item.type.message.name;
		case INT: return "int";
		case LONG: return "long";
		case DOUBLE: return "double";
		case FLOAT: return "float";
		default: throw new Error("unpossible");
		}
	}
	
	public String defaultVal() {
		return item.defaultVal().map(MessageItem::renderLiteral)
		.orElseGet(()->{
			if (item.repeated()) return "[]";
			switch (item.decodedType().builtIn) {
			case INT32: case INT64: case SINT32: case SINT64: return "0";
			case BOOL: return "false";
			case STRING: return "\"\"";
			case ENUM: return "0";
			case MESSAGE: return "undefined";
			default: return "";
			}
		});
	}

	private static String renderLiteral(Literal l) {
		switch(l.type) {
		case BOOLEAN: return l.boolVal ? "true" : "false";
		case STRING: return "\"" + l.string.replace("\"", "\\\"") + "\"";
		default: return "" + l.intVal;
		}
	}
	
}