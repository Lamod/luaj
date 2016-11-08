package lamo.luaj.parser.ast;

import lamo.luaj.base.LBoolean;
import lamo.luaj.base.LValue;

public class True extends KExpr {

	public String toCode() {
		return "true";
	}

	public LValue toLuaValue() {
		return LBoolean.TRUE;
	}

}
