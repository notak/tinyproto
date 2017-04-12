package me.taks.proto;

import java.util.ArrayList;
import java.util.List;

public class Package extends Type {
	public List<String> imports = new ArrayList<>();
	
	public Package(String name) {
		super(null, null, name);
	}
}