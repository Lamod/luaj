package lamo.luaj.util;

public class BoolUtil {

	public static int toInt(boolean b) {
		return b ? 1 : 0;
	}

	public static int invert(boolean b) {
		return b ? 0 : 1;
	}

}
