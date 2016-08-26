package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class LocalStat extends Stat {

	private String[] names;
	private Expr[] explist;

	public String[] getNames() {
		return names;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public Expr[] getExplist() {
		return explist;
	}

	public void setExplist(Expr[] explist) {
		this.explist = explist;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(getIntend());
		sb.append("local ");
		
		sb.append(ArrayUtil.join(this.names, ", "));

		if (explist != null && explist.length > 0) {
			sb.append(" = ");
			sb.append(ArrayUtil.join(this.explist, CODE_SERIALIZOR, ", "));
		}
		sb.append("\n");
			
		return sb.toString();
	}
}
