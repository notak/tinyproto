package me.taks.proto;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import static me.taks.proto.Message.BuiltIn.*;
import static me.taks.proto.Message.Field.Scope.*;
import me.taks.proto.Type.ProtoEnum;

class ModelBuilderTest {

	@Test
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
		assertEquals(
				"test", pkg.name, "Package name is correctly set");
		assertEquals(
				1, pkg.childMessages().count(), "Message was created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals(
				"msg", msg.name, "Message name is correctly set");
		assertEquals(
				4, msg.items.size(), "All fields were created");
		Message.Field i = msg.items.get(0);
		assertEquals(OPTIONAL, i.scope, "Simple Int is optional");
		assertEquals(INT32, i.type.builtIn, "Simple Int is int32");
		assertEquals(5, i.number, "Simple Int is id 5");
		assertEquals(
				"simpleInt", i.name, "Simple Int is called simpleInt");
		i = msg.items.get(1);
		assertEquals(REQUIRED, i.scope, "Required String is required");
		assertEquals(STRING, i.type.builtIn, "Required String is string");
		assertEquals(3, i.number, "Required String is id 3");
		assertEquals(
				"reqString", i.name, "Required String is called reqString");
		i = msg.items.get(2);
		assertEquals(REPEATED, i.scope, "Repeated String is repeated");
		assertEquals(STRING, i.type.builtIn, "Repeated String is string");
		assertEquals(1, i.number, "Repeated String is id 1");
		assertEquals(
				"repeatedStrings", i.name, "Repeated String is called repeatedStrings");
		i = msg.items.get(3);
		assertEquals(PACKED, i.scope, "Packed Signed is packed");
		assertEquals(SINT32, i.type.builtIn, "Packed Signed is sint32");
		assertEquals(14, i.number, "Packed Signed is id 14");
		assertEquals(
				"packedSigned", i.name, "Packed Signed is called packedSigned");
	}
	
	@Test
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
		assertEquals(1, pkg.childMessages().count(), "Message was created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals(6, msg.items.size(), "All fields were created");
		assertEquals(INT64, msg.items.get(0).type.builtIn);
		assertEquals(UINT64, msg.items.get(1).type.builtIn);
		assertEquals(SINT64, msg.items.get(2).type.builtIn);
		assertEquals(INT32, msg.items.get(3).type.builtIn);
		assertEquals(UINT32, msg.items.get(4).type.builtIn);
		assertEquals(SINT32, msg.items.get(5).type.builtIn);
	}

	@Test
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
		assertEquals(1, pkg.childMessages().count(), "Message was created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals(4, msg.items.size(), "All fields were created");
		assertEquals(FIXED32, msg.items.get(0).type.builtIn);
		assertEquals(SFIXED32, msg.items.get(1).type.builtIn);
		assertEquals(FIXED64, msg.items.get(2).type.builtIn);
		assertEquals(SFIXED64, msg.items.get(3).type.builtIn);
	}

	@Test
	public void testFloating() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional float f = 6;"
			+ "optional double d = 7;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals(1, pkg.childMessages().count(), "Message was created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals(2, msg.items.size(), "All fields were created");
		assertEquals(FLOAT, msg.items.get(0).type.builtIn);
		assertEquals(DOUBLE, msg.items.get(1).type.builtIn);
	}

	@Test
	public void testStringBoolBytes() throws IOException {
		ModelBuilder mb = new ModelBuilder().build(
			"package test;\n"
			+ "message msg {"
			+ "optional string s = 6;"
			+ "optional bool b = 7;"
			+ "optional bytes by = 8;"
		+ "}");
		Package pkg = mb.pkg();
		assertEquals(1, pkg.childMessages().count(), "Message was created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals(3, msg.items.size(), "All fields were created");
		assertEquals(STRING, msg.items.get(0).type.builtIn);
		assertEquals(BOOL, msg.items.get(1).type.builtIn);
		assertEquals(BYTES, msg.items.get(2).type.builtIn);
	}

	@Test
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
		assertEquals(2, pkg.childMessages().count(), "Messages were created");
		Message msg = pkg.childMessages().findFirst().get();
		Message msg2 = pkg.childMessages().skip(1).findFirst().get();
		assertEquals(1, msg.childEnums().count(), "msg has an enum");
		ProtoEnum testEnum = msg.childEnums().findFirst().get();
		Message.Field i = msg2.items.get(0);
		assertEquals(msg2, i.message, "msg is attached to message");
		assertEquals(OPTIONAL, i.scope, "msg is optional");
		assertEquals(COMPLEX, i.type.builtIn, "msg is complex");
		assertEquals("Msg", i.type.complex, "msg is called Msg");
		assertEquals(msg, i.type.complex(), "msg is a Msg");
		assertEquals(9, i.number, "msg is id 9");
		i = msg2.items.get(1);
		assertEquals(REQUIRED, i.scope, "testEnum is required");
		assertEquals(COMPLEX, i.type.builtIn, "testEnum is complex");
		assertEquals("Msg.Enum", i.type.complex, "testEnum is called Msg.Enum");
		assertEquals(testEnum, i.type.complex(), "testEnum is an Enum");
		assertEquals(3, i.number, "testEnum is id 3");
		
		assertEquals(3, testEnum.items.size(), "enum has 3 values");
		assertEquals(1, testEnum.items.get("FIRST").intValue(), "enum FIRST = 1");
		assertEquals(2, testEnum.items.get("SECOND").intValue(), "enum SECOND = 2");
		assertEquals(3, testEnum.items.get("THIRD").intValue(), "enum THIRD = 3");
	}
	
	@Test
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
		assertEquals(1, msg.childEnums().count(), "msg has an enum");
		ProtoEnum testEnum = msg.childEnums().findFirst().get();
		Message.Field i = msg.items.get(0);
		assertEquals(COMPLEX, i.type.builtIn, "msg is complex");
		assertEquals("Msg2", i.type.complex, "msg is called Msg2");
		assertEquals(msg2, i.type.complex(), "msg is a Msg2");
		i = msg.items.get(1);
		assertEquals(COMPLEX, i.type.builtIn, "testEnum is complex");
		assertEquals("Enum", i.type.complex, "testEnum is called Enum");
		assertEquals(testEnum, i.type.complex(), "testEnum is an Enum");
	}	
	
	@Test
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
		assertEquals(1, pkg.childMessages().count(), "Messages were created");
		Message msg = pkg.childMessages().findFirst().get();
		assertEquals("Another option", msg.unknownOpts.get("myMessageHas"));
		Message.Field i = msg.items.get(0);
		assertEquals(PACKED, i.scope, "packed");
		assertEquals(8, i.subtract);
		assertEquals(4, i.divisor);
		assertEquals("Pob.Explode", i.encoding);
		assertEquals("I can haz", i.unknownOpts.get("biffy"));
	}

}
