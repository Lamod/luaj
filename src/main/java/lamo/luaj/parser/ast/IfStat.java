package lamo.luaj.parser.ast;

import java.util.ArrayList;
import lamo.luaj.util.ArrayUtil;

public class IfStat extends Stat {

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

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		String intend = getIntend();
		Branch branch = this.branches.get(0);

		sb.append(intend);
		sb.append("if ");
		sb.append(branch.getCondition().toCode());
		sb.append(" then\n");
		sb.append(branch.getBlock().toCode());

		for (int i = 1; i < this.branches.size(); ++i) {
			branch = this.branches.get(i);
			sb.append(intend);
			if (branch.isElseBranch()) {
				sb.append("else\n");
			} else {
				sb.append("elseif ");
				sb.append(branch.getCondition().toCode());
				sb.append(" then");
			}
			sb.append(branch.getBlock().toCode());
		}

		sb.append(intend);
		sb.append("end\n");

		return sb.toString();
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

		public boolean isElseBranch() {
			return condition == null;
		}

	}

}
