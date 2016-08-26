package lamo.luaj.parser.ast;

public class WhileStat extends Stat {

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

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		String intend = getIntend();

		sb.append(intend);
		sb.append("while ");
		sb.append(this.condition.toCode());
		sb.append(" do\n");
		sb.append(this.block.toCode());
		sb.append(intend);
		sb.append("end\n");

		return sb.toString();
	}

}
