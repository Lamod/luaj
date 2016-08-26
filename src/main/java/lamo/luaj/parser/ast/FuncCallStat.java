package lamo.luaj.parser.ast;

public class FuncCallStat extends Stat {

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

	public String toCode() {
		return getIntend() + this.expr.toCode() + "\n";
	}

}
