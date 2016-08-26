package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class GenericForStat extends Stat {

	private String[] nameList;
	private Expr[] exprList;
	private Block block;

	public GenericForStat(String[] nameList, Expr[] exprList, Block block) {
		this.nameList = nameList;
		this.exprList = exprList;
		this.block = block;
	}

	public String[] getNameList() {
		return this.nameList;
	}

	public void setNameList(String[] nameList) {
		this.nameList = nameList;
	}

	public Expr[] getExprList() {
		return this.exprList;
	}

	public void setExprList(Expr[] exprList) {
		this.exprList = exprList;
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
		sb.append(ArrayUtil.join(this.nameList, ", "));
		sb.append(" in ");
		sb.append(ArrayUtil.join(this.exprList, CODE_SERIALIZOR, ", "));
		sb.append(" do\n");
		sb.append(this.block.toCode());
		sb.append(intend);
		sb.append("end\n");

		return sb.toString();
	}

}
