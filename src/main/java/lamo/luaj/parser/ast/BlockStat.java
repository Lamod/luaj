package lamo.luaj.parser.ast;

public class BlockStat extends Stat {

	private Block block;

	public BlockStat(Block block) {
		this.block = block;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();

		sb.append(getIntend() + "do\n");
		sb.append(this.block.toCode());
		sb.append(getIntend() + "end\n");

		return sb.toString();
	}

}
