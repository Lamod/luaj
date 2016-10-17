package lamo.luaj.translator;

class Scope {

	int startOfActVar;
	boolean hasUpvalue;

	final boolean breakable;

	Scope(boolean breakable) {
		this.breakable = breakable;
	}

}
