package lamo.luaj.parser.ast;

import lamo.luaj.LValue;

public abstract class KExpr extends Expr {

	public abstract LValue toLuaValue();

}
