	export interface Supplier<T> { (): T }
	
	export class Parser<T> {
		public _out: T;
		public buf: DataView;
		public start: number = 0;
		public end: number = 0;

		constructor() {}

		getIdByte() {
			this.buf.getUint8(this.start++);
		}

		getVarInt(zigZag = false) { //TODO: assumes fits in a signed number
			var n = 0;
			var i=0;
			do {
				var byte = this.buf.getUint8(this.start++);
				n += ((byte & 127) << (7*i++));
			} while ((byte & 128) > 0);
			return zigZag ? (n >> 1) ^ (-(n & 1)) : n;
		}

		getBytes(len: number) {
			return new Uint8Array(this.buf.buffer, this.start, len);
		}

		getString(len: number) {
			return String.fromCodePoint.apply(null, this.getBytes(len));
		}

		getPacked<T>(len: number, fn: Supplier<T>) {
			var out:any = [];
			var start = this.start;
			while (this.start < start + len) out.push(fn());
			this.start = start; //shouldn't have moved pointer forward...
			return out;
		}

		getFixed(bytes: number) {
			let out=0;
			//TODO: assumes a 64 bit number fits
			for (let i=0; i<bytes; i++) out += this.buf.getUint8(this.start++) << i*8;
			return out;
		}

		protected startDecode() { }

		protected endDecode() { }

		decode(buf: DataView, start?: number, end?: number): T {
			this.buf = buf;
			this.start = start || 0;
			this.end = end || buf.byteLength;

			this.startDecode();

			while (this.start < this.end) {
				var id = this.getVarInt();
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

		parse(buf: ArrayBuffer) {
			return this.decode(new DataView(buf));
		}

		process(field: number, lenOrVal: number) {
		}
	}

	export interface CallbackListParserCallback {
		(buf: ArrayBuffer, id: number): void
	}

	export class CallbackListParser extends Parser<any> {
		constructor(private cb: CallbackListParserCallback) {
			super();
		}
		
	    process(field: number, lenOrVal: number) {
			//assumes all fields are fixed width
			this.cb(this.buf.buffer.slice(
				this.start + this.buf.byteOffset,
				this.start + this.buf.byteOffset + lenOrVal
			), field); 
		}
	}
	
	export class Builder {
		private data: Uint8Array; //dont use this for large data...
		private end = 0;

		constructor(size = 128) {
			this.data = new Uint8Array(size);
		}

		private double() {
			var data = new Uint8Array(this.data.length * 2)
			data.set(this.data);
			this.data = data;
		}

		private set(n: number, pos = -1) {
			if (pos<0) pos = this.end;
			if (pos >= this.data.length) this.double();
			this.data[pos++] = n;
			if (pos > this.end) this.end = pos;
		}

		private add(n: number, zigZag=false, pos=-1) {
			if (zigZag) n = (n << 1) ^ (n > 0 ? 0 : 1);
			do { this.set(n & 127 | ((n = n >> 7) ? 128 : 0), pos); } while (n);
		}

		setString(id: number, s: string|undefined) { //todo: only handles single-bit characters
			if (s !== undefined) {
				this.add((id << 3) | 2);
				this.add(s.length);
				for (var i = 0; i < s.length; i++) this.set(s.charCodeAt(i));
			}
			return this;
		}

		setStrings(id: number, n: string[]|undefined) {
			if (n !== undefined) {
				n.forEach(i=>this.setString(id, i));
			}
			return this;
		}

		setVarInt(id: number, n: number|undefined, zigZag = false) {
			if (n !== undefined) {
				this.add(id << 3);
				this.add(n, zigZag);
			}
			return this;
		}

		setVarInts(id: number, n: number[]|undefined, zigZag = false) {
			if (n !== undefined) {
				n.forEach(i=>this.setVarInt(id, i, zigZag));
			}
			return this;
		}

		setArrayBuffer(id: number, d: ArrayBuffer|undefined) {
			if (d && d.byteLength) {
				var b = new Uint8Array(d);
				this.add((id << 3) | 2);
				this.add(b.length);
				while (this.end + b.length > this.data.length) this.double();
				this.data.set(b, this.end);
				this.end += b.length;
			}
			return this;
		}

		setBuilder(id: number, d: Builder|undefined) {
			if (d !== undefined) this.setArrayBuffer(id, d.toArrayBuffer());
			return this;
		}

		setBuilders(id: number, d: Builder[]) {
			d.forEach(b=>this.setBuilder(id, b));
			return this;
		}

		setPackedVarInts(id: number, d: number[], zigZag = false) {
			if (d && d.length) {
				this.add((id << 3) | 2);
				var start = ++this.end;
				var b = new Uint8Array(d);
				for (var i = 0; i < d.length; i++) this.add(d[i], zigZag);
				var sizeSize = -1;
				for (var len = this.end - start; len; len = len >> 7) sizeSize++;
				if (sizeSize) {
					if (this.data.length <= this.end + sizeSize) this.double();
					this.data.copyWithin(start, this.end, start + sizeSize);
					this.end += sizeSize;
				}
			}
			return this;
		}

		toArrayBuffer() {
			return new Uint8Array(this.data.slice(0, this.end)).buffer;
		}
	}
