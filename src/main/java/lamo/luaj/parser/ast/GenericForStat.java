package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class GenericForStat extends Stat {

	private String[] names;
	private Expr[] exprs;
	private Block block;

	public GenericForStat(String[] names, Expr[] exprs, Block block) {
		this.names = names;
		this.exprs = exprs;
		this.block = block;
	}

	public String[] getNames() {
		return this.names;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public Expr[] getExprs() {
		return this.exprs;
	}

	public void setExprs(Expr[] exprs) {
		this.exprs = exprs;
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
		sb.append(ArrayUtil.join(this.names, ", "));
		sb.append(" in ");
		sb.append(ArrayUtil.join(this.exprs, CODE_SERIALIZER, ", "));
		sb.append(" do\n");
		sb.append(this.block.toCode());
		sb.append(intend);
		sb.append("end\n");

		return sb.toString();
	}

}
