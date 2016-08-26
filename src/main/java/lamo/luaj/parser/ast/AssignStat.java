package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class AssignStat extends Stat {

	private PrimaryExpr[] variables;
	private Expr[] values;

	public PrimaryExpr[] getVariables() {
		return this.variables;
	}

	public void setVariables(PrimaryExpr[] variables) {
		this.variables = variables;
	}

	public Expr[] getValues() {
		return this.values;
	}

	public void setValues(Expr[] values) {
		this.values = values;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getIntend());

		sb.append(ArrayUtil.join(this.variables, CODE_SERIALIZOR, ", "));
		if (this.values == null) {
			return sb.toString();
		} 

		sb.append(" = ");
		sb.append(ArrayUtil.join(this.values, CODE_SERIALIZOR, ", "));
		sb.append("\n");

		return sb.toString();
	}

}
