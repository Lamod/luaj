package lamo.luaj;

public class LString extends LValue {

	private String string;

	public LString() { }

	public LString(String str) {
		this.string = str;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (o.getClass() != this.getClass()) {
			return false;
		}

		LString s = (LString)o;
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
