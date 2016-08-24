package lamo.luaj.parser.ast;

public class FuncCallStat implements Stat {

	private PrimaryExpr expr;

	public FuncCallStat(PrimaryExpr expr) {
		setExpr(expr);
	}

	public PrimaryExpr getExpr() {
		return this.expr;
	}

	public void setExpr(PrimaryExpr expr) {
		assert(expr != null && expr.isFuncCallExpr());
		this.expr = expr;
	}

}
