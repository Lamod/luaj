package lamo.luaj;

public class LBoolean extends LValue {

	public static LBoolean TRUE = new LBoolean();
	public static LBoolean FALSE = new LBoolean();

	private LBoolean() { }

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
