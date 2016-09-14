package lamo.luaj;

public class LNil extends LValue {

	public static LNil NIL = new LNil();

	public boolean equals(LValue v) {
		if (v instanceof LNil) {
			return equals((LNil)v);
		} else {
			return false;
		}
	}

	public boolean equals(LNil nil) {
		return nil == this;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
