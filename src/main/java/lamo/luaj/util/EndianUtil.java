package lamo.luaj.util;

public class EndianUtil {

	public static byte[] littleEndian(int i) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)(i & 0xff);
		bytes[1] = (byte)((i >>> 8) & 0xff);
		bytes[2] = (byte)((i >>> 16) & 0xff);
		bytes[3] = (byte)((i >>> 24) & 0xff);

		return bytes;
	}

	public static byte[] littleEndian(long l) {
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

	public static byte[] littleEndian(float f) {
		return littleEndian(Float.floatToIntBits(f));
	}

	public static byte[] littleEndian(double d) {
		return littleEndian(Double.doubleToLongBits(d));
	}

}
