package lamo.luaj.translator;

import lamo.luaj.parser.ast.Expr;

public class AssignVarInfo {

	static final int LOCAL = 1;		// info = reg
	static final int UPVALUE = 2;	// info = index
	static final int GLOBAL = 3;	// info = RK(name)
	static final int TABLE= 4;		// info = index of table

	int type;
	int info;
	Expr key;

	static AssignVarInfo local(int reg) {
		AssignVarInfo info = new AssignVarInfo();
		info.type = LOCAL;
		info.info = reg;
		return info;
	}

	static AssignVarInfo upvalue(int index) {
		AssignVarInfo info = new AssignVarInfo();
		info.type = UPVALUE;
		info.info = index;
		return info;
	}

	static AssignVarInfo global(int name) {
		AssignVarInfo info = new AssignVarInfo();
		info.type = GLOBAL;
		info.info = name;
		return info;
	}

	static AssignVarInfo table(int table, Expr key) {
		AssignVarInfo info = new AssignVarInfo();
		info.type = TABLE;
		info.info = table;
		info.key = key;
		return info;
	}

}
