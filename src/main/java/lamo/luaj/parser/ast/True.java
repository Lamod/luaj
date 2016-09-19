package lamo.luaj.parser.ast;

import lamo.luaj.LBoolean;
import lamo.luaj.LValue;

public class True extends KExpr {

	public String toCode() {
		return "true";
	}

	public LValue toLuaValue() {
		return LBoolean.TRUE;
	}

}
