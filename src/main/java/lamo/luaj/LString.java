package lamo.luaj;

public class LString extends LValue {

	private String string;

	public LString() { }

	public LString(String str) {
		this.string = str;
	}

	public boolean equals(LValue v) {
		if (v instanceof LString) {
			return equals((LString)v);
		} else {
			return false;
		}
	}

	public boolean equals(LString s) {
		if (s.string == this.string) {
			return true;
		}
		if (s.string == null || this.string == null) {
			return false;
		}
		return s.string.equals(this.string);
	}

	@Override
	public int hashCode() {
		return this.string == null ? 17 : this.string.hashCode();
	}

}