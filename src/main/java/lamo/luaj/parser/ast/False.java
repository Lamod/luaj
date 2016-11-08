package lamo.luaj.parser.ast;

import lamo.luaj.base.LBoolean;
import lamo.luaj.base.LValue;

public class False extends KExpr {

	public String toCode() {
		return "false";
	}

	public LValue toLuaValue() {
		return LBoolean.FALSE;
	}

}
