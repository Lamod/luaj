package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class LocalStat extends Stat {

	private String[] names;
	private Expr[] exprs;
	private boolean accessable; // names are accessable in exprs?

	public String[] getNames() {
		return names;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public Expr[] getExprs() {
		return exprs;
	}

	public void setExprs(Expr[] exprs) {
		this.exprs = exprs;
	}

	public boolean isAccessable() {
		return accessable;
	}

	public void setAccessable(boolean accessable) {
		this.accessable = accessable;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getIntend());
		sb.append("local ");

		String names = ArrayUtil.join(this.names, ", ");
		sb.append(names);

		if (this.exprs != null && this.exprs.length > 0) {
			if (this.accessable) {
				sb.append("\n");
				sb.append(getIntend());
				sb.append(names);
			}
			sb.append(" = ");
			sb.append(ArrayUtil.join(this.exprs, CODE_SERIALIZER, ", "));
		}
		sb.append("\n");
			
		return sb.toString();
	}

}
