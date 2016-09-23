package lamo.luaj;

public class LNil extends LValue {

	public static LNil NIL = new LNil();

	private LNil() { }

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
