package me.taks.proto;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import me.taks.proto.Literal.Types;

public class Field {
	
	public enum Scope { 
		NONE, REQUIRED, OPTIONAL, REPEATED, PACKED 
	}
	public enum Options { 
		DIVISOR, SUBTRACT, GROUPING, ENCODING, DEFAULT, DECODED_TYPE;

		public static Options fromString(String in) {
			try { return Options.valueOf(in); }
			catch (Exception e) { return null; }
		}
	}

	public Scope scope;
	public final FieldType type;
	public final String name;
	public final int number;
	public final Map<Options, Literal> opts;
	public final Map<String, Literal> unknownOpts;

	public Field(
		Scope scope, FieldType type, String name, int number,
		Map<Options, Literal> opts, Map<String, Literal> unknownOpts
	) {
		this.scope = scope;
		this.type = type;
		this.name = name;
		this.number = number;
		this.opts = opts;
		this.unknownOpts = unknownOpts;
	}
	
	public Field(Field.Scope scope, FieldType type, String name, int number) {
		this(scope, type, name, number, Collections.emptyMap(), Collections.emptyMap());
	}
	
	@SuppressWarnings("unchecked")
	public static<K,V> Map<K, V>appended(Map<K, V> map, K key, V val) {
		var entries = map.entrySet().toArray(new Map.Entry[map.size()+1]);
		entries[map.size()] = Map.entry(key, val);
		return Map.ofEntries(entries);
	}
	
	public Optional<Literal> option(Options key) {
		return Optional.ofNullable(opts.get(key));
	}

	public Optional<Long> intOption(Options key) {
		return option(key)
		.filter(o->o.type==Literal.Types.INT)
		.map(o->o.intVal);
	}

	public Field withOption(String key, Literal value) {
		return new Field(scope, type, name, number, opts, 
			appended(unknownOpts, key, value));
	}

	public Field withOption(Options key, Literal value) {
		return new Field(scope, type, name, number, 
			appended(opts, key, value), unknownOpts);
	}

	public FieldType decodedType() {
		var o = opts.getOrDefault(Options.DECODED_TYPE, Literal.EMPTY);
		return o.type==Types.STRING ? new FieldType(o.string) : type;
	}

	public boolean repeated() {
		return scope==Scope.REPEATED || scope==Scope.PACKED;
	}

	public Optional<Literal> defaultVal() {
		return option(Options.DEFAULT);
	}

	public Optional<FieldType> encoding() {
		return option(Options.DECODED_TYPE)
		.filter(o->o.type==Types.STRING)
		.map(o->new FieldType(o.string));
	}
}