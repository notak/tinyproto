package me.taks.proto;

import java.util.HashMap;
import java.util.Map;

public class Package {
	public String name;
	public Map<String, Message> messages = new HashMap<>();
}