package lamo.luaj;

public class LBoolean extends LValue {

	public static LBoolean TRUE = new LBoolean();
	public static LBoolean FALSE = new LBoolean();

	public LBoolean() {
		assert(false);
	}

	public boolean equals(LValue v) {
		if (v instanceof LBoolean) {
			return equals((LBoolean)v);
		} else {
			return false;
		}
	}

	public boolean equals(LBoolean b) {
		return b == this;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
