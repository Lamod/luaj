package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class FuncBody extends Node {

	private Parlist parlist;
	private Chunk chunk;
	private boolean needSelf;

	public void setParlist(Parlist parlist) {
		this.parlist = parlist;
	}

	public Parlist getParlist() {
		return parlist;
	}

	public void setChunk(Chunk chunk) {
		this.chunk = chunk;
	}

	public Chunk getChunk() {
		return chunk;
	}

	public boolean isNeedSelf() {
		return needSelf;
	}

	public void setNeedSelf(boolean needSelf) {
		this.needSelf = needSelf;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (this.parlist != null) {
			sb.append(this.parlist.toCode());
		}
		sb.append(")");
		sb.append("\n");
		sb.append(this.chunk.toCode());

		return sb.toString();
	}

	public static class Parlist extends Node {
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

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			if (this.params != null) {
				sb.append(ArrayUtil.join(this.params, CODE_SERIALIZOR, ", "));
			}
			if (this.isVarargs) {
				if (this.params != null) {
					sb.append(", ");
				}
				sb.append("...");
			}

			return sb.toString();
		}
				
	}

}
