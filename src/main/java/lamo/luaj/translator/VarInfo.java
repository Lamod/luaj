package lamo.luaj.translator;

class VarInfo {

	static final int LOCAL = 0;
	static final int UPVALUE = 1;
	static final int GLOBAL = 2;

	static final VarInfo singleton = new VarInfo();

	int type;
	int index;

}
