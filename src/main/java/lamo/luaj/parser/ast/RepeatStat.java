package lamo.luaj.parser.ast;

public class RepeatStat implements Stat {

	private Expr condition;
	private Block block;

	public RepeatStat(Expr condition, Block block) {
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
