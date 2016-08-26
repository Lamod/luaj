package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class LocalStat extends Stat {

	private String[] names;
	private Expr[] exprs;

	public String[] getNames() {
		return names;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public Expr[] getExprs() {
		return exprs;
	}

	public void setExprs(Expr[] exprs) {
		this.exprs = exprs;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getIntend());
		sb.append("local ");
		
		sb.append(ArrayUtil.join(this.names, ", "));

		if (this.exprs != null && this.exprs.length > 0) {
			sb.append(" = ");
			sb.append(ArrayUtil.join(this.exprs, CODE_SERIALIZOR, ", "));
		}
		sb.append("\n");
			
		return sb.toString();
	}
}
