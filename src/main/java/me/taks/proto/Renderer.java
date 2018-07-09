package me.taks.proto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.util.Arrays.stream;
import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.taks.proto.Field.Scope;

abstract public class Renderer {
	protected String out;

	class Include {
		protected List<String> list;
		protected boolean exclude;
	}
	public boolean includes(String category, String s) {
		Include i = includes.get(category);
		return i==null || i.exclude != i.list.contains(s);
	}
	
	protected Map<String, Include> includes = new HashMap<>();
	
	public Renderer set(String[] parts, String value) {
		boolean exclude = false;
		
		switch (parts[1]) {
		case "out": out = value; break;
		case "exclude": 
			exclude = true; //DFF
		case "include":
			Include i = includes.computeIfAbsent(parts[2], (k)->new Include());
			i.exclude = exclude;
			i.list = asList(value.split(","));
			break;
		}
		return this;
	}
	
	public String renderScope(String syntax, Scope scope) {
		return scope==Scope.PACKED ? "repeated "
				: scope==Scope.NONE ? ""
				: scope.toString().toLowerCase() + " ";
	}
	
	public Stream<Output> renderContent(Package p) {
		return Stream.of(
			stream(p.msgs).filter(s->includes("classes", s.name))
			.map(m->renderClass(p, m)).flatMap(x->x),
			stream(p.msgs).filter(s->includes("builders", s.name))
			.map(m->renderBuilder(p, m)).flatMap(x->x),
			stream(p.msgs).filter(s->includes("parsers", s.name))
			.map(m->renderParser(p, m)).flatMap(x->x)
		).flatMap(x->x);
	}
	
	Stream<Output> renderClass(Package p, Message m) { 
		return Stream.empty(); 
	}
	Stream<Output> renderBuilder(Package p, Message m) { 
		return Stream.empty(); 
	}
	Stream<Output> renderParser(Package p, Message m) { 
		return Stream.empty(); 
	}

	abstract public Stream<Output> render(Package pkg);

	public void write(Package pkg) {
		System.out.println("Writing file "+out);
		try {
			Files.write(Paths.get(out),
					render(pkg).flatMap(o->o.lines("\t"))
					.collect(Collectors.joining("\n")).getBytes()
			);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
