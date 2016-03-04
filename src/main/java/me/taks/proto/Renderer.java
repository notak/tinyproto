package me.taks.proto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract public class Renderer {
	protected String out;
	public Renderer set(String key, String value) {
		switch (key) {
		case "out": out = value; break;
		}
		return this;
	}
	
	abstract Stream<Output> render(Package pkg);

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
