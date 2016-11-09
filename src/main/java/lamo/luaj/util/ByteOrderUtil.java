package lamo.luaj.util;

import java.nio.ByteOrder;

public class ByteOrderUtil {

	public static final boolean littleEndian =
		(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

	public static byte[] toBytes(short s, boolean le) {
		byte[] bytes = toBytes(s);
		return le == littleEndian ? bytes : reverse(bytes);
	}

	public static byte[] toBytes(int i, boolean le) {
		byte[] bytes = toBytes(i);
		return le == littleEndian ? bytes : reverse(bytes);
	}

	public static byte[] toBytes(long l, boolean le) {
		byte[] bytes = toBytes(l);
		return le == littleEndian ? bytes : reverse(bytes);
	}

	public static byte[] toBytes(float f, boolean le) {
		byte[] bytes = toBytes(f);
		return le == littleEndian ? bytes : reverse(bytes);
	}

	public static byte[] toBytes(double d, boolean le) {
		byte[] bytes = toBytes(d);
		return le == littleEndian ? bytes : reverse(bytes);
	}

	public static byte[] reverse(byte[] bytes) {
		if (bytes == null) {
			return bytes;
		}

		byte tmp;
		int l = bytes.length;
		for (int i = 0; i < (l + 1) / 2; ++i) {
			tmp = bytes[i];
			bytes[i] = bytes[l - i - 1];
			bytes[l - i - 1] = tmp;
		}

		return bytes;
	}

	public static byte[] toBytes(short s) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte)(s & 0xff);
		bytes[1] = (byte)((s >>> 8) & 0xff);

		return bytes;
	}

	public static byte[] toBytes(int i) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)(i & 0xff);
		bytes[1] = (byte)((i >>> 8) & 0xff);
		bytes[2] = (byte)((i >>> 16) & 0xff);
		bytes[3] = (byte)((i >>> 24) & 0xff);

		return bytes;
	}

	public static byte[] toBytes(long l) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte)(l & 0xff);
		bytes[1] = (byte)((l >>> 8) & 0xff);
		bytes[2] = (byte)((l >>> 16) & 0xff);
		bytes[3] = (byte)((l >>> 24) & 0xff);
		bytes[4] = (byte)((l >>> 32) & 0xff);
		bytes[5] = (byte)((l >>> 40) & 0xff);
		bytes[6] = (byte)((l >>> 48) & 0xff);
		bytes[7] = (byte)((l >>> 56) & 0xff);

		return bytes;
	}

	public static byte[] toBytes(float f) {
		return toBytes(Float.floatToIntBits(f));
	}

	public static byte[] toBytes(double d) {
		return toBytes(Double.doubleToLongBits(d));
	}

}
