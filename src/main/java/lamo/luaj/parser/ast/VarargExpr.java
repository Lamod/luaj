package lamo.luaj.parser.ast;

public class VarargExpr extends Expr {

	public String toCode() {
		return "...";
	}

	public boolean hasMultRet() {
		return true;
	}

}
