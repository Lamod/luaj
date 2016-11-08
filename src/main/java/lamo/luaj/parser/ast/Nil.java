package lamo.luaj.parser.ast;

import lamo.luaj.base.LNil;
import lamo.luaj.base.LValue;

public class Nil extends KExpr {

	public String toCode() {
		return "nil";
	}

	public LValue toLuaValue() {
		return LNil.NIL;
	}

}
