package lamo.luaj;

import lamo.luaj.base.*;
import lamo.luaj.util.ArrayUtil;
import lamo.luaj.util.ByteOrderUtil;
import lamo.luaj.vm.Instruction;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Dumper {

	private Proto p;
	private DataOutputStream out;
	private boolean strip;

	static public byte[] dump(Proto p, boolean strip) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (new Dumper(p, out, strip).dump()) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return out.toByteArray();
		} else {
			return null;
		}
	}

	static public byte[] header() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		boolean succeed = false;
		try {
			header(out);
			succeed = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bytes.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return succeed ? bytes.toByteArray() : null;
	}

	public Dumper(Proto p, OutputStream out, boolean strip) {
		this.p = p;
		this.out = new DataOutputStream(out);
		this.strip = strip;
	}

	public boolean dump() {
		try {
			header(this.out);
			function(this.p, this.strip ? null : this.p.getSource());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	static private void header(DataOutputStream out) throws IOException {
		writeUTF(out, Lua.SIGNATURE, false);
		out.writeByte(Luac.VERSION);
		out.writeByte(Luac.FORMAT);
		out.writeBoolean(Config.littleEndian); // is little endian?
		out.writeByte(4); // size of int
		out.writeByte(8); // size of size_t
		out.writeByte(4); // size of Instruction
		out.writeByte(8); // size of Number
		out.writeByte(0); // Number is not integral
	}

	private void function(Proto p, String source) throws IOException {
		dumpString(source);
		dumpInt(p.getLineDefined());
		dumpInt(p.getLastLineDefined());
		dumpByte(ArrayUtil.sizeOf(p.getUpValues()));
		dumpByte(p.getNumParams());
		dumpBoolean(p.isVararg());
		dumpByte(p.getMaxStackSize());
		code(p.getCode());
		constants(p);
		debug(p);
	}

	private void code(Instruction[] code) throws IOException {
		dumpArray(code, e -> dumpInt(e.getValue()));
	}

	private void debug(Proto p) throws IOException {
		if (this.strip) {
			for (int i = 0; i < 3; ++i) {
				dumpInt(0);
			}
		} else {
			dumpInt(0); // TODO: lineinfo
			dumpArray(p.getLocVars(), v -> {
				dumpString(v.getName());
				dumpInt(v.getStartPC());
				dumpInt(v.getEndPC());
			});
			dumpArray(p.getUpValues(), this::dumpString);
		}
	}

	private void constants(Proto p) throws IOException {
		dumpArray(p.getKs(), v -> {
			dumpByte(v.getType());
			if (v instanceof LBoolean) {
				dumpBoolean(((LBoolean)v).getValue());
			} else if (v instanceof LNumber) {
				dumpNumber(((LNumber)v).getValue());
			} else if (v instanceof LString) {
				dumpString(((LString)v).getString());
			} else {
				assert v instanceof LNil;
			}
		});

		dumpArray(p.getPs(), e -> function(e, null));
	}

	private void dumpInt(int i) throws IOException {
		this.out.write(ByteOrderUtil.toBytes(i, Config.littleEndian));
	}

	private void dumpByte(int b) throws IOException {
		this.out.writeByte(b);
	}

	private void dumpString(String s) throws IOException {
		if (s == null) {
			dumpSize(0);
		} else {
			dumpSize(s.length() + 1);
			writeUTF(this.out, s, true);
		}
	}

	private void dumpSize(int s) throws IOException {
		this.out.write(ByteOrderUtil.toBytes((long)s, Config.littleEndian));
	}

	private void dumpNumber(double n) throws IOException {
		this.out.write(ByteOrderUtil.toBytes(n, Config.littleEndian));
	}

	private void dumpBoolean(boolean b) throws IOException {
		this.out.writeBoolean(b);
	}

	private <T> void dumpArray(T[] arr, Enumerator<T> enumerator) throws IOException {
		if (ArrayUtil.isEmpty(arr)) {
			dumpInt(0);
			return;
		}

		dumpInt(ArrayUtil.sizeOf(arr));
		for (T e : arr) {
			enumerator.dump(e);
		}
	}

	private interface Enumerator<T> {
		void dump(T e) throws IOException;
	}

	private static void writeUTF(OutputStream out, String s, boolean term) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(bout);
		dout.writeUTF(s);
		byte[] bytes = bout.toByteArray();

		out.write(bytes, 2, bytes.length - 2);
		if (term) {
			out.write(0);
		}
	}

}
