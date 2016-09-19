package lamo.luaj.parser.ast;

import lamo.luaj.LBoolean;
import lamo.luaj.LValue;

public class False extends KExpr {

	public String toCode() {
		return "false";
	}

	public LValue toLuaValue() {
		return LBoolean.FALSE;
	}

}
