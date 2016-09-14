package lamo.luaj;

public abstract class LValue {

	public boolean equals(Object o) {
		if (o instanceof LValue) {
			return equals((LValue)o);
		} else {
			return false;
		}
	}

	abstract public boolean equals(LValue v);

}
