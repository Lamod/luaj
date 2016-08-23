package lamo.luaj.parser.ast;

import java.util.ArrayList;

public class IfStat implements Stat {

	private ArrayList<Branch> branches = new ArrayList<Branch>();

	public ArrayList<Branch> getBranches() {
		return branches;
	}

	public int append(Branch branch) {
		if (branches.add(branch)) {
			return branches.size() - 1;
		} else {
			return -1;
		}
	}

	public int append(Expr condition, Block block) {
		return append(new Branch(condition, block));
	}

	static public class Branch {
		
		private Expr condition;
		private Block block;

		public Branch(Expr condition, Block block) {
			this.condition = condition;
			this.block = block;
		}

		public Branch(Block block) {
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

		public boolean isLastBranch() {
			return condition == null;
		}

	}

}
