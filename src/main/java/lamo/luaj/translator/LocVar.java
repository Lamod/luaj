package lamo.luaj.translator;

import lamo.luaj.base.Proto;

class LocVar {

	String name;
	int startPC, endPC;

	LocVar(String name) {
		this.name = name;
	}

	Proto.LocVar toProtoLocVar() {
		return new Proto.LocVar(this.name, this.startPC, this.endPC);
	}

}
