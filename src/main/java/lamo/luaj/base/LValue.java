package lamo.luaj.base;

public abstract class LValue {

	static public final int NONE = -1;
	static public final int NIL = 0;
	static public final int BOOLEAN = 1;
	static public final int LIGHTUSERDATA = 2;
	static public final int NUMBER = 3;
	static public final int STRING = 4;
	static public final int TABLE = 5;
	static public final int FUNCTION = 6;
	static public final int USERDATA = 7;
	static public final int THREAD = 8;

	final public int getType() {
		if (this instanceof LNil) {
			return NIL;
		} else if (this instanceof LBoolean) {
			return BOOLEAN;
		} else if (this instanceof LNumber) {
			return NUMBER;
		} else if (this instanceof LString) {
			return STRING;
		} else {
			return NONE;
		}
	}

}

