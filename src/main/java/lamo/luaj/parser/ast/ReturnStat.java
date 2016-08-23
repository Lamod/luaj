package lamo.luaj.parser.ast;

public class ReturnStat implements LastStat {

	private Expr[] exprList;

	public ReturnStat(Expr[] exprList) {
		this.exprList = exprList;
	}

	public Expr[] getExprList() {
		return this.exprList;
	}

	public void setExprList() {
		this.exprList = exprList;
	}

}
