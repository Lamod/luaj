package lamo.luaj.parser.ast;

import java.util.Arrays;

public class Block extends Node {

	private int level;
	private String intend;
	private Block parent;

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

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int level) {
		this.level = level;
		if (level == 0) {
			this.intend = "";
		} else {
			char[] spaces = new char[this.level * INDENT_SIZE];
			Arrays.fill(spaces, ' ');
			this.intend = new String(spaces);
		}
	}

	public String getIntend() {
		return this.intend;
	}

	public Block getParent() {
		return this.parent;
	}

	public void setParent(Block parent) {
		this.parent = parent;
	}

	public String toCode() {
		if (statements == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Stat stat: statements) {
			sb.append(stat.toCode());
		}

		return sb.toString();
	}

}
