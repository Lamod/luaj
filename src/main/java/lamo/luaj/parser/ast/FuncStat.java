package lamo.luaj.parser.ast;

public class FuncStat extends Stat {

	private FuncName name = new FuncName();
	private FuncBody body;

	public FuncName getName() {
		return this.name;
	}

	public void setName(FuncName name) {
		this.name = name;
	}

	public FuncBody getBody() {
		return this.body;
	}

	public void setBody(FuncBody body) {
		this.body = body;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getIntend());
		sb.append("function ");
		sb.append(this.name.toCode());
		sb.append(this.body.toCode());
		sb.append(getIntend());
		sb.append("end\n");

		return sb.toString();
	}

	static public class FuncName extends Node {

		private String[] segments;
		private boolean needSelf;

		public String[] getSegments() {
			return this.segments;
		}

		public void setSegments(String[] segments) {
			this.segments = segments;
		}

		public boolean getNeedSelf() {
			return this.needSelf;
		}

		public void setNeedSelf(boolean needSelf) {
			this.needSelf = needSelf;
		}

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.segments.length; ++i) {
				if (i > 0) {
					if (i == this.segments.length - 1 && this.needSelf) {
						sb.append(":");
					} else {
						sb.append(".");
					}
				}
				sb.append(this.segments[i]);
			}

			return sb.toString();
		}

	}

}
