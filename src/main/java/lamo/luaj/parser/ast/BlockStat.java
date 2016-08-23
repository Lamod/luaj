package lamo.luaj.parser.ast;

public class BlockStat implements Stat {

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

}
