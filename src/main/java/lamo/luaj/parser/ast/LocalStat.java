package lamo.luaj.parser.ast;

public class LocalStat implements Stat {

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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("local ");
		
		for (int i = 0; i < names.length; ++i) {
			if (i != 0) {
				sb.append(", ");
			}
			sb.append(names[i]);
		}

		if (explist != null && explist.length > 0) {
			sb.append(" = ");

			for (int i = 0; i < explist.length; ++i) {
				if (i != 0) {
					sb.append(", ");
				}
				sb.append(explist[i].toString());
			}
		}
			
		return sb.toString();
	}
}
