package me.taks.proto;

import java.io.IOException;

import junit.framework.TestCase;
import me.taks.proto.Type.Enum;
import static me.taks.proto.Message.FieldType.BuiltIn.*;
import static me.taks.proto.Message.Field.Scope.*;

public class ModelBuilderTest extends TestCase {
	public void testBasic() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
				"package test;\n"
				+ "message msg {"
				+ "optional int32 simpleInt = 5;"
				+ "required string reqString = 3;"
				+ "repeated string repeatedStrings = 1;"
				+ "repeated sint32 packedSigned = 14 [packed=true];"
		+ "}");
		
		Package pkg = mb.pkg();
		assertEquals("Package name is correctly set", 
				"test", pkg.name);
		assertEquals("Message was created", 
				1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("Message name is correctly set", 
				"msg", msg.name);
		assertEquals("All fields were created", 
				4, msg.items.size());
		Message.Field i = msg.items.get(0);
		assertEquals("Simple Int is optional", OPTIONAL, i.scope);
		assertEquals("Simple Int is int32", INT32, i.type.builtIn);
		assertEquals("Simple Int is id 5", 5, i.number);
		assertEquals("Simple Int is called simpleInt", 
				"simpleInt", i.name);
		i = msg.items.get(1);
		assertEquals("Required String is required", REQUIRED, i.scope);
		assertEquals("Required String is string", STRING, i.type.builtIn);
		assertEquals("Required String is id 3", 3, i.number);
		assertEquals("Required String is called reqString", 
				"reqString", i.name);
		i = msg.items.get(2);
		assertEquals("Repeated String is repeated", REPEATED, i.scope);
		assertEquals("Repeated String is string", STRING, i.type.builtIn);
		assertEquals("Repeated String is id 1", 1, i.number);
		assertEquals("Repeated String is called repeatedStrings", 
				"repeatedStrings", i.name);
		i = msg.items.get(3);
		assertEquals("Packed Signed is packed", PACKED, i.scope);
		assertEquals("Packed Signed is sint32", SINT32, i.type.builtIn);
		assertEquals("Packed Signed is id 14", 14, i.number);
		assertEquals("Packed Signed is called packedSigned", 
				"packedSigned", i.name);
	}
	
	public void testVarInts() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional int64 testInt64 = 6;"
			+ "optional uint64 testUInt64 = 7;"
			+ "optional sint64 testSInt64 = 8;"
			+ "optional int32 testInt32 = 9;"
			+ "optional uint32 testUInt32 = 10;"
			+ "optional sint32 testSInt32 = 11;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals("Message was created", 1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("All fields were created", 6, msg.items.size());
		assertEquals(INT64, msg.items.get(0).type.builtIn);
		assertEquals(UINT64, msg.items.get(1).type.builtIn);
		assertEquals(SINT64, msg.items.get(2).type.builtIn);
		assertEquals(INT32, msg.items.get(3).type.builtIn);
		assertEquals(UINT32, msg.items.get(4).type.builtIn);
		assertEquals(SINT32, msg.items.get(5).type.builtIn);
	}

	public void testFixed() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional fixed32 f32 = 6;"
			+ "optional sfixed32 sf32 = 7;"
			+ "optional fixed64 f64 = 8;"
			+ "optional sfixed64 sf64 = 9;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals("Message was created", 1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("All fields were created", 4, msg.items.size());
		assertEquals(FIXED32, msg.items.get(0).type.builtIn);
		assertEquals(SFIXED32, msg.items.get(1).type.builtIn);
		assertEquals(FIXED64, msg.items.get(2).type.builtIn);
		assertEquals(SFIXED64, msg.items.get(3).type.builtIn);
	}

	public void testFloating() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional float f = 6;"
			+ "optional double d = 7;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals("Message was created", 1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("All fields were created", 2, msg.items.size());
		assertEquals(FLOAT, msg.items.get(0).type.builtIn);
		assertEquals(DOUBLE, msg.items.get(1).type.builtIn);
	}

	public void testStringBoolBytes() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional string s = 6;"
			+ "optional bool b = 7;"
			+ "optional bytes by = 8;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals("Message was created", 1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("All fields were created", 3, msg.items.size());
		assertEquals(STRING, msg.items.get(0).type.builtIn);
		assertEquals(BOOL, msg.items.get(1).type.builtIn);
		assertEquals(BYTES, msg.items.get(2).type.builtIn);
	}

	public void testEnumAndGlobalMessage() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
				"package test;\n"
				+ "message Msg {"
				+ "enum Enum { FIRST = 1; SECOND = 2; THIRD = 3;}"
				+ "}"
				+ "message Msg2 {"
				+ "optional Msg msg = 9;"
				+ "required Msg.Enum testEnum = 3;"
		+ "}");

		Package pkg = mb.pkg();
		assertEquals("Messages were created", 2, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		Message msg2 = pkg.childMessages().skip(1).findFirst().get();
		assertEquals("msg has an enum", 1, msg.childEnums().count());
		Enum testEnum = msg.childEnums().findFirst().get();
		Message.Field i = msg2.items.get(0);
		assertEquals("msg is attached to message", msg2, i.message);
		assertEquals("msg is optional", OPTIONAL, i.scope);
		assertEquals("msg is complex", COMPLEX, i.type.builtIn);
		assertEquals("msg is called Msg", "Msg", i.type.complex);
		assertEquals("msg is a Msg", msg, i.type.complex());
		assertEquals("msg is id 9", 9, i.number);
		i = msg2.items.get(1);
		assertEquals("testEnum is required", REQUIRED, i.scope);
		assertEquals("testEnum is complex", COMPLEX, i.type.builtIn);
		assertEquals("testEnum is called Msg.Enum", "Msg.Enum", i.type.complex);
		assertEquals("testEnum is an Enum", testEnum, i.type.complex());
		assertEquals("testEnum is id 3", 3, i.number);
		
		assertEquals("enum has 3 values", 3, testEnum.items.size());
		assertEquals("enum FIRST = 1", 1, testEnum.items.get("FIRST").intValue());
		assertEquals("enum SECOND = 2", 2, testEnum.items.get("SECOND").intValue());
		assertEquals("enum THIRD = 3", 3, testEnum.items.get("THIRD").intValue());
	}
	
	public void testLocalMessage() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
				"package test;\n"
				+ "message Msg {"
				+ "enum Enum { FIRST = 1; SECOND = 2; THIRD = 3;}"
				+ "message Msg2 {"
				+ "optional Msg msg = 9;"
				+ "required Msg.Enum testEnum = 3;"
				+ "}"
				+ "optional Msg2 msg = 9;"
				+ "required Enum testEnum = 3;"
		+ "}");

		Package pkg = mb.pkg();
		Message msg = pkg.childMessages().findFirst().get();
		Message msg2 = msg.childMessages().findFirst().get();
		assertEquals("msg has an enum", 1, msg.childEnums().count());
		Enum testEnum = msg.childEnums().findFirst().get();
		Message.Field i = msg.items.get(0);
		assertEquals("msg is complex", COMPLEX, i.type.builtIn);
		assertEquals("msg is called Msg2", "Msg2", i.type.complex);
		assertEquals("msg is a Msg2", msg2, i.type.complex());
		i = msg.items.get(1);
		assertEquals("testEnum is complex", COMPLEX, i.type.builtIn);
		assertEquals("testEnum is called Enum", "Enum", i.type.complex);
		assertEquals("testEnum is an Enum", testEnum, i.type.complex());
	}	
	
	public void testOptions() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
				"package test;\n"
				+ "option myPackageHas=\"One option\";"
				+ "message Msg {"
				+ "option myMessageHas=\"Another option\";"
				+ "repeated int32 msg = 9[packed=true,"
					+ "biffy=\"I can haz\","
					+ "subtract=8,"
					+ "divide=4,"
					+ "encoding=\"Pob.Explode\"];"
				+ "}");
		Package pkg = mb.pkg();
		assertEquals("One option", pkg.unknownOpts.get("myPackageHas"));
		assertEquals("Messages were created", 1, pkg.childMessages().count());
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("Another option", msg.unknownOpts.get("myMessageHas"));
		Message.Field i = msg.items.get(0);
		assertEquals("packed", PACKED, i.scope);
		assertEquals(8, i.subtract);
		assertEquals(4, i.divisor);
		assertEquals("Pob.Explode", i.encoding);
		assertEquals("I can haz", i.unknownOpts.get("biffy"));
	}
}
