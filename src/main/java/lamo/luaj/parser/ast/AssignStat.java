package lamo.luaj.parser.ast;

public class AssignStat implements Stat {

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

}
