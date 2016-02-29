package me.taks.proto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Output {
	public String head;
	public String lineEnd = ";";
	public String startBrace = "{";
	public String endBrace = "}";
	public String emptyBody = "{}";
	public List<Output> children = new ArrayList<>();
	public List<String> lines = new ArrayList<>();
	
	public Output head(String head) {
		this.head = head;
		return this;
	}
	
	public Output emptyBody(String empty) {
		this.emptyBody = empty;
		return this;
	}
	
	public Output line(String line) {
		if (line!=null) this.lines.add(line);
		return this;
	}
	
	public Output lines(Stream<String> lines) {
		lines.forEach(this.lines::add);
		return this;
	}
	
	public Output child(Output child) {
		this.children.add(child);
		return this;
	}
	
	public Output children(Stream<Output> children) {
		children.forEach(this.children::add);
		return this;
	}
	
	public Stream<String> lines(String indent) {
		if (children.isEmpty() && lines.isEmpty()) {
			return Stream.of(head + emptyBody);
		} else return Stream.of(
			Stream.of(head + " " + startBrace), 
			children.stream().flatMap(c->c.lines(indent)).map(s->indent + s),
			lines.stream().map(l->indent + l + lineEnd),	
			Stream.of(endBrace),
			Stream.of("")
		).flatMap(x->x);
	}
}