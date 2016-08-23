package lamo.luaj.parser.ast;

public class FuncStat implements Stat {

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

	static public class FuncName {

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

	}

}
