package lamo.luaj.parser.ast;

public class RepeatStat extends Stat {

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

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		String intend = getIntend();
		
		sb.append(intend);
		sb.append("repeat\n");
		sb.append(this.block.toCode());
		sb.append(intend);
		sb.append("until ");
		sb.append(this.condition.toCode());
		sb.append("\n");

		return sb.toString();
	}

}
