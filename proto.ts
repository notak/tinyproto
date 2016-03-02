module proto {
	"use strict"
	
	export class Parser<T> {
		public _out: T;
		public buf: DataView;
		public start: number;
		public end: number;

		constructor(protected root?: Parser<any>) {}

		getIdByte() {
			this.buf.getUint8(this.start++);
		}

		getVarInt(zigZag?: boolean) { //TODO: assumes fits in a signed number
			var n = 0;
			var i=0;
			do {
				var byte = this.buf.getUint8(this.start++);
				n += ((byte & 127) << (7*i++));
			} while ((byte & 128) > 0);
			return zigZag ? (n >> 1) ^ (-(n & 1)) : n;
		}

		getString(len: number) {
			var uints = new Uint8Array(this.buf.buffer, this.start, len);
			return String.fromCodePoint.apply(null, uints);
		}

		getPacked(len: number, fn: Function) {
			var out = [];
			var start = this.start;
			while (this.start < start + len) out.push(fn());
			this.start = start; //shouldn't have moved pointer forward...
			return out;
		}

		protected startDecode() { }

		protected endDecode() { }

		// This should be called on the root parser;
		// if a superclass is defined in the map, use that. Otherwise use the provided type
		protected getParser(type: typeof Parser) {
			return this.root ? this.root.getParser(type) : new type(this);
		}

		decode(buf: DataView, start?: number, end?: number): T {
			this.buf = buf;
			this.start = start || 0;
			this.end = end || buf.byteLength;

			this.startDecode();

			while (this.start < this.end) {
				var id = buf.getUint8(this.start++);
				var value = 0;
				var length = 0;
				switch (id & 7) {
					case 0: length = 0; value = this.getVarInt(); break;
					case 1: length = 8; break;
					case 2: length = this.getVarInt(); break;
					case 5: length = 4; break;
					default:
						console.log("Unexpected type " + (id & 7) + " for field type " + (id >> 3));
						console.trace();
				}
				this.process(id >> 3, value || length);
				this.start += length;
			}
			this.endDecode();
			return this._out;
		}

		process(field: number, lenOrVal: number) {
		}
	}

	export class Builder {
		private data = []; //dont use this for large data...
		public start: number;

		addVarInt(n: number, zigZag?: boolean) {
			if (zigZag) n = (n << 1) ^ (n > 0 ? 0 : 1);
			while (n) {
				var out = n & 127;
				n = n >> 7;
				this.data.push(out | (n ? 128 : 0));
			}
		}

		setString(id: number, s: string) { //todo: only handles single-bit characters
			if (s !== undefined) {
				this.addVarInt((id << 3) | 2);
				this.addVarInt(s.length);
				for (var i = 0; i < s.length; i++) this.data.push(s.charCodeAt(i));
			}
			return this;
		}

		setVarInt(id: number, n: number, zigZag?: boolean) {
			if (n !== undefined) {
				this.addVarInt(id << 3);
				this.addVarInt(n, zigZag);
			}
			return this;
		}

		toArrayBuffer() {
			return Uint8Array.from(this.data).buffer;
		}
	}
}
