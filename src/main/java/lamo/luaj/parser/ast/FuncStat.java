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

	public class FuncName extends Node {

		private Var var;
		private String[] fields;

		public Var getVar() {
			return var;
		}

		public void setVar(Var var) {
			this.var = var;
		}

		public String[] getFields() {
			return this.fields;
		}

		public void setFields(String[] fields) {
			this.fields = fields;
		}

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			boolean needSelf = getBody().isNeedSelf();
			sb.append(this.var.getName());
			if (this.fields != null) {
				for (int i = 0; i < this.fields.length; ++i) {
					if (i == this.fields.length - 1 && needSelf) {
						sb.append(":");
					} else {
						sb.append(".");
					}
					sb.append(this.fields[i]);
				}
			}

			return sb.toString();
		}

	}

}
