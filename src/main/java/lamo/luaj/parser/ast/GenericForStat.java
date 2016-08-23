package lamo.luaj.parser.ast;

public class GenericForStat implements Stat {

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

}
