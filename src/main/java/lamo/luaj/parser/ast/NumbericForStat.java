package lamo.luaj.parser.ast;

public class NumbericForStat extends Stat {

	private String varName;
	private Expr initExpr, limitExpr, stepExpr;
	private Block block;

	public NumbericForStat(String varName,
			Expr initExpr, Expr limitExpr, Expr stepExpr, Block block) {
		this.varName = varName;
		this.initExpr = initExpr;
		this.limitExpr = limitExpr;
		this.stepExpr = stepExpr;
		this.block = block;
	}

	public String getVarName() {
		return this.varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public Expr getInitExpr() {
		return this.initExpr;
	}

	public void setInitExpr(Expr initExpr) {
		this.initExpr = initExpr;
	}

	public Expr getLimitExpr() {
		return this.limitExpr;
	}

	public void setLimitExpr(Expr limitExpr) {
		this.limitExpr = limitExpr;
	}

	public Expr getStepExpr() {
		return this.stepExpr;
	}

	public void setStepExpr(Expr stepExpr) {
		this.stepExpr = stepExpr;
	}

	public Block getBlock() {
		return this.block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		String intend = getIntend();

		sb.append(intend);
		sb.append("for ");
		sb.append(this.varName);
		sb.append(" = ");
		sb.append(this.initExpr.toCode());
		sb.append(", ");
		sb.append(this.limitExpr.toCode());
		if (this.stepExpr != null) {
			sb.append(", ");
			sb.append(this.stepExpr.toCode());
		}
		sb.append(" do\n");
		sb.append(this.block.toCode());
		sb.append(intend);
		sb.append("end\n");

		return sb.toString();
	}

}
