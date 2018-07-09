package me.taks.proto;

import java.util.Optional;
import static me.taks.proto.Utils.*;

public class Package extends Message {
	public final String[] imports;
	public String syntax = "";
	
	public Package(String name, String[] imports) {
		super(null, name);
		this.imports = imports;
	}

	public Package(
		String[] imports, String name, Field[] items, ProtoEnum[] enums, 
		Message[] msgs
	) {
		super(null, name, items, enums, msgs);
		this.imports = imports;
	}

	public Package withMessage(Message msg) {
		return new Package(imports, name, items, enums, append(msgs, msg));
	}

	public Package withEnum(ProtoEnum p) {
		return new Package(imports, name, items, append(enums, p), msgs);
	}

	public Optional<Message> resolveMessage(String typeName) {
		return resolveDown(typeName);
	}
	
	@Override
	public String fullName() {
		return name;
	}
}