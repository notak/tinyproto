package me.taks.proto;

public class Package extends Type {
	public final String[] imports;
	public String syntax = "";
	
	public Package(String name, ProtoEnum[] enums, String[] imports) {
		super(null, name, enums);
		this.imports = imports;
	}
}