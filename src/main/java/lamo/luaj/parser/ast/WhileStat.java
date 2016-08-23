package lamo.luaj.parser.ast;

public class WhileStat implements Stat {

	private Expr condition;
	private Block block;

	public WhileStat(Expr condition, Block block) {
		this.condition = condition;
		this.block = block;
	}

	public Expr getCondition() {
		return condition;
	}

	public void setCondition(Expr condition) {
		this.condition = condition;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

}
