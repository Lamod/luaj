package lamo.luaj.parser.ast;

public class Chunk {

	private int level;
	private Stat[] statements;
	private LastStat lastStat;

	public Stat[] getStatements() {
		return statements;
	}

	public void setStatements(Stat[] statements) {
		this.statements = statements;
	}

	public LastStat getLastStat() {
		return this.lastStat;
	}

	public void setLastStat(LastStat lastStat) {
		this.lastStat = lastStat;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	public String toString() {
		if (statements == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Stat stat: statements) {
			sb.append(stat.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

}
