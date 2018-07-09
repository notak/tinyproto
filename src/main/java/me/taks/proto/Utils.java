package me.taks.proto;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Utils {
	public static String appendIfNonEmpty(String in, String toAppend) {
		return  in.length()==0 ? in : in + toAppend;
	}

	public static<T> T[] append(T[] haystack, T needle) {
		var out = Arrays.copyOf(haystack, haystack.length+1);
		out[haystack.length] = needle;
		return out;
	}
	
	public static<T, V> Optional<T> matching(Stream<T> s, Function<T, V> map, V eq) {
		return s.filter(t->eq.equals(map.apply(t))).findAny();
	}
}
