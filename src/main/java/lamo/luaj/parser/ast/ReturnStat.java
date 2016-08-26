package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class ReturnStat extends LastStat {

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

	public String toCode() {
		StringBuilder sb = new StringBuilder();

		sb.append(getIntend());
		sb.append("return ");
		if (this.exprList != null) {
			sb.append(ArrayUtil.join(this.exprList, CODE_SERIALIZOR, ", "));
		}

		return sb.toString();
	}

}
