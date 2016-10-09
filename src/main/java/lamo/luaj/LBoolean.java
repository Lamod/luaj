package lamo.luaj;

public class LBoolean extends LValue {

	public static LBoolean TRUE = new LBoolean();
	public static LBoolean FALSE = new LBoolean();

	private LBoolean() { }

	public boolean getValue() {
		if (this == TRUE) {
			return true;
		} else if (this == FALSE) {
			return false;
		} else {
			assert false;
			return false;
		}
	}

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
