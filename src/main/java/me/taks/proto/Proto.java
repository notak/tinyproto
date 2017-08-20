package me.taks.proto;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Proto {
	private static final Charset UTF8 = Charset.forName("UTF8");
	
	@FunctionalInterface
	static interface Factory<T> {
		public Parser<T> get(Parser<T> in);
	}

	abstract public static class Parser<T> {
		public T _out;
		public byte[] buf;
		public int start;
		public int end;
		protected Factory<?> root;
		public Consumer<String> log = s->{};
		

		Parser() {
			this(c->c);
		}
		
		<X> Parser(Factory<X> root) {
			this.root = root;
		}
		
		protected int getIdByte() {
			return buf[this.start++];
		}

		protected int varInt() { 
			return varInt(false);
		}
		protected int varInt(boolean zigZag) { 
			int n = 0;
			int i=0;
			byte b;
			do {
				b = buf[this.start++];
				n += ((b & 127) << (7*i++));
			} while ((b & 128) > 0);
			return zigZag ? (n >> 1) ^ (-(n & 1)) : n;
		}

		protected int varLong(boolean zigZag) { //TODO: assumes fits in a signed number
			int n = 0;
			int i=0;
			byte b;
			do {
				b = buf[this.start++];
				n += ((b & 127) << (7*i++));
			} while ((b & 128) > 0);
			return zigZag ? (n >> 1) ^ (-(n & 1)) : n;
		}

		private byte[] bytes(int len) {
			return Arrays.copyOfRange(buf, start, len + start);
		}

		protected String string(int len) {
			return new String(bytes(len), UTF8);
		}

		protected <R> List<R> getPacked(int len, Supplier<R> fn) {
			List<R> out = new ArrayList<>();
			int dataStart = start;
			while (start < dataStart + len) out.add(fn.get());
			start = dataStart; //shouldn't have moved pointer forward...
			return out;
		}

		protected int getFixed32(int bytes) {
			int out=0;
			//TODO: assumes a 64 bit number fits
			for (int i=0; i<bytes; i++) out += buf[start++] << i*8;
			return out;
		}

		protected long getFixed64(int bytes) {
			long out=0;
			//TODO: assumes a 64 bit number fits
			for (int i=0; i<bytes; i++) out += buf[start++] << i*8;
			return out;
		}

		protected void startDecode() {}
		protected void endDecode() { }

		public T decode(byte[] buf) {
			return decode(buf, 0);
		}
		public T decode(byte[] buf, int start) {
			return decode(buf, start, buf.length);
		}
		public T decode(byte[] buf, int start, int end) {
			this.buf = buf;
			this.start = start;
			this.end = end;
			return doDecode();
		}

		public T doDecode() {
			startDecode();

			while (start < end) {
				int id = buf[start++];
				int value = 0;
				int length = 0;
				switch (id & 7) {
					case 0: length = 0; value = varInt(); break;
					case 1: length = 8; break;
					case 2: length = this.varInt(); break;
					case 5: length = 4; break;
					default:
						log.accept(
							"Unexpected type " + (id & 7) + 
							" for field type " + (id >> 3));
				}
				process(id >> 3, value!=0 ? value : length);
				this.start += length;
			}
			endDecode();
			return this._out;
		}

		abstract void process(int field, int lenOrVal);
	}

	public static class Builder {
		private byte[] data; //dont use this for large data...
		private int end = 0;

		Builder(int size) {
			data = new byte[size];
		}

		private void embiggen() {
			data = Arrays.copyOf(data, data.length * 2);
		}

		private void set(byte n) {
			set(n, end);
		}
		private void set(byte n, int pos) {
			if (pos >= this.data.length) embiggen();
			data[pos++] = n;
			if (pos > this.end) this.end = pos;
		}

		private void writeVarInt(long n) {
			writeVarInt(n, false);
		}
		private void writeVarInt(long n, boolean zigZag) {
			writeVarInt(n, zigZag, end);
		}
		private void writeVarInt(long n, boolean zigZag, int pos) {
			if (zigZag) n = (n << 1) ^ (n > 0 ? 0 : 1);
			do { set((byte)(n & 127 | ((n = (n >>> 7))!=0 ? 128 : 0)), pos); } 
			while (n!=0);
		}

		public Builder bytes(int id, byte[] b) { //todo: only handles single-bit characters
			writeVarInt((id << 3) | 2);
			writeVarInt(b.length);
			for (int i = 0; i < b.length; i++) set(b[i]);
			return this;
		}

		public Builder string(int id, String s) { //todo: only handles single-bit characters
			return bytes(id, s.getBytes(UTF8));
		}

		public Builder strings(int id, String[] n) {
			for (String s: n) string(id, s);
			return this;
		}

		public Builder varInt(int id, int n, boolean zigZag) {
			writeVarInt(id << 3);
			writeVarInt(n, zigZag);
			return this;
		}

		public Builder varInts(int id, int n[], boolean zigZag) {
			for (int i: n) varInt(id, i, zigZag);
			return this;
		}

		public Builder builder(int id, Builder d) {
			this.bytes(id, d.bytes());
			return this;
		}

		public Builder packedVarInts(int id, int d[], boolean zigZag) {
			//TODO: It would be more graceful to just build the byte array
			if (d.length>0) {
				writeVarInt((id << 3) | 2);
				int dataStart = ++end;
				for (int i : d) writeVarInt(i, zigZag);
				int sizeSize = -1;
				int len = end - dataStart;
				for (int decLen = len; decLen!=0; decLen >>>= 7) sizeSize++;
				if (sizeSize>0) {
					for (int i=len-1; i>=0; i--) {
						set(data[dataStart+i], dataStart+i+sizeSize);
					}
					writeVarInt(len, false, dataStart-1);
				}
			}
			return this;
		}

		public byte[] bytes() {
			return Arrays.copyOf(data, end);
		}
	}
}
