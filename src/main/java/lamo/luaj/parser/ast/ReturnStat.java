package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class ReturnStat extends LastStat {

	private Expr[] exprs;

	public ReturnStat(Expr[] exprs) {
		this.exprs = exprs;
	}

	public Expr[] getExprs() {
		return this.exprs;
	}

	public void setExprs(Expr[] exprs) {
		this.exprs = exprs;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();

		sb.append(getIntend());
		sb.append("return ");
		if (this.exprs != null) {
			sb.append(ArrayUtil.join(this.exprs, CODE_SERIALIZOR, ", "));
		}

		return sb.toString();
	}

}
