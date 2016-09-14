package lamo.luaj;

public class LNumber extends LValue {

	private double value;

	public LNumber() {
		this.value = 0.0;
	}

	public LNumber(double value) {
		this.value = value;
	}

	public boolean equals(LValue v) {
		if (v instanceof LNumber) {
			return equals((LNumber)v);
		} else {
			return false;
		}
	}

	public boolean equals(LNumber n) {
		return n.value == this.value;
	}

	@Override
	public int hashCode() {
		return new Double(this.value).hashCode();
	}

}
