package lamo.luaj.parser.ast;

public class FuncBody implements Expr {

	private Parlist parlist;
	private Block block;

	public void setParlist(Parlist parlist) {
		this.parlist = parlist;
	}

	public Parlist getParlist() {
		return parlist;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public Block getBlock() {
		return block;
	}

	public static class Parlist {
		private String[] params;
		private boolean isVarargs;

		public void setParams(String[] params) {
			this.params = params;
		}

		public String[] getParams() {
			return params;
		}

		public void setIsVarargs(boolean isVarargs) {
			this.isVarargs = isVarargs;
		}

		public boolean getIsVarargs() {
			return isVarargs;
		}
	}

}
