package lamo.luaj.base;

public class LNumber extends LValue {

	private double value;

	public LNumber() {
		this.value = 0.0;
	}

	public LNumber(double value) {
		this.value = value;
	}

	public double getValue() {
		return this.value;
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
		return ((LNumber)o).value == this.value;
	}

	@Override
	public int hashCode() {
		return new Double(this.value).hashCode();
	}

}
