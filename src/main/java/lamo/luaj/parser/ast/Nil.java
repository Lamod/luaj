package lamo.luaj.parser.ast;

import lamo.luaj.LNil;
import lamo.luaj.LValue;

public class Nil extends KExpr {

	public String toCode() {
		return "nil";
	}

	public LValue toLuaValue() {
		return LNil.NIL;
	}

}
