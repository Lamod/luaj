package lamo.luaj.translator;

import java.util.ArrayList;

class Scope {

	int startOfActVar;
	boolean hasUpvalue;
	ArrayList<Integer> breakList;

	final boolean breakable;

	Scope(boolean breakable) {
		this.breakable = breakable;
	}

	int addBreakPoint(int jmp) {
		if (this.breakList == null) {
			this.breakList = new ArrayList<>();
		}
		this.breakList.add(jmp);

		return this.breakList.size() - 1;
	}

}
