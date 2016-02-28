package me.taks.proto;

import java.util.stream.Collectors;

import junit.framework.TestCase;

public class TypeScriptRendererTest extends TestCase {

	public void testOutput() {
		Output out = new Output();
		out.lineEnd = "*";
		out.startBrace = "[";
		out.endBrace = "]";
		out.head = "ME HEAD";
		Output outChild = new Output();
		outChild.head = "CHILD1";
		outChild.lines.add("THIS IS CHILD LINE");
		out.children.add(outChild);
		out.children.add(outChild);
		out.lines.add("I'M A LINE");
		out.lines.add("ME TOO");
		assertEquals(
			"ME HEAD [,  CHILD1 {,    THIS IS CHILD LINE;,  },  CHILD1 {,    "
			+ "THIS IS CHILD LINE;,  },  I'M A LINE*,  ME TOO*,]", 
			out.lines("  ").collect(Collectors.joining(","))
		);
	}

}
