package lamo.luaj.vm;

import lamo.luaj.LValue;

public class Proto {

	private final Instruction[] code;
	private final Proto[] ps;
	private final LocVar[] locVars;
	private final String[] upValues;
	private final LValue[] ks;
	private final int numParams;
	private final boolean vararg;

	public Proto(Instruction[] code, Proto[] ps,
			LocVar[] locVars, String[] upValues, LValue[] ks,
			int numParams, boolean vararg) {
		this.code = code;
		this.ps = ps;
		this.locVars = locVars;
		this.upValues = upValues;
		this.ks = ks;
		this.numParams = numParams;
		this.vararg = vararg;
	}

	public Instruction[] getCode() {
		return this.code;
	}

	public Proto[] getPs() {
		return this.ps;
	}

	public LocVar[] getLocVars() {
		return this.locVars;
	}

	public String[] getUpValues() {
		return this.upValues;
	}

	public LValue[] getKs() {
		return this.ks;
	}

	public int getNumParams() {
		return numParams;
	}

	public boolean isVararg() {
		return vararg;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.code != null) {
			for (Instruction i : this.code) {
				sb.append(i.toString());
				sb.append("\n");
			}
		}
		for (Proto p : this.ps) {
			sb.append("\n");
			sb.append(p.toString());
		}

		return sb.toString();
	}

	static public class LocVar {

		private String name;
		private int startPC, endPC;

		public LocVar(String name, int startPC, int endPC) {
			this.name = name;
			this.startPC = startPC;
			this.endPC = endPC;
		}

		public String getName() {
			return this.name;
		}

		public int getStartPC() {
			return this.startPC;
		}

		public int getEndPC() {
			return this.endPC;
		}

	}

}
