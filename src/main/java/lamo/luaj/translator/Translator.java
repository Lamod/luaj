package lamo.luaj.translator;

import lamo.luaj.LNumber;
import lamo.luaj.LString;
import lamo.luaj.LValue;
import lamo.luaj.parser.ast.*;
import lamo.luaj.vm.Instruction;
import lamo.luaj.vm.OpCode;
import lamo.luaj.vm.Proto;

import java.util.ArrayList;
import java.util.LinkedList;

public class Translator {

	private Chunk chunk;
	private Translator parent;
	private ArrayList<Translator> children = new ArrayList<>();

	private ArrayList<LocVar> localVars = new ArrayList<>();
	private LinkedList<Integer> actVars = new LinkedList<>();
	private int freeReg;

	private LinkedList<Scope> scopes = new LinkedList<>();

	private ArrayList<Proto> ps = new ArrayList<>();
	private ArrayList<String> upValueNemes = new ArrayList<>();
	private ArrayList<LValue> ks = new ArrayList<>();

	private ArrayList<Instruction> code = new ArrayList<>();

	public Translator(Chunk chunk) {
		this.chunk = chunk;
	}

	public Translator(Chunk chunk, Translator parent) {
		this.chunk = chunk;
		this.parent = parent;
		if (parent != null) {
			parent.children.add(this);
		}
	}

	public Chunk getChunk() {
		return this.chunk;
	}

	public Translator getParent() {
		return this.parent;
	}

	public Translator[] getChildren() {
		return this.children.toArray(new Translator[this.children.size()]);
	}

	public Proto translat() {
		clean();

		openScope();
		for (Stat stat : this.chunk.getStatements()) {
			translatStat(stat);
			this.freeReg = this.actVars.size();
		}

		closeScope();
		return toProto();
	}

	public void clean() {
		this.localVars.clear();
		this.actVars.clear();
		this.freeReg = 0;

		this.scopes.clear();
		this.ps.clear();
		this.upValueNemes.clear();
		this.ks.clear();

		this.code.clear();
	}

	public String toString() {
		return toProto().toString();
	}

	private Proto toProto() {
		Instruction[] is = this.code.toArray(new Instruction[this.code.size()]);
		Proto[] ps = this.ps.toArray(new Proto[this.ps.size()]);
		Proto.LocVar[] ls = new Proto.LocVar[this.localVars.size()];
		for (int i = 0; i < this.localVars.size(); ++i) {
			ls[i] = this.localVars.get(i).toProtoLocVar();
		}
		LValue[] ks = this.ks.toArray(new LValue[this.ks.size()]);

		return new Proto(is, ps, ls, null, ks);
	}

	private void translatStat(Stat stat) {
		if (stat instanceof LocalStat) {
			LocalStat ls = (LocalStat)stat;
			String[] names = ls.getNames();
			for (String n : names) {
				addLocalVar(n);
			}
			Expr[] es = ls.getExprs();
			if (es != null) {
				for (Expr e : es) {
					if (e == es[es.length - 1]) {
						adjustAssign(names.length, es.length, e);
					} else {
						translatExpr(e);
					}
				}
			} else {
				adjustAssign(names.length, 0);
			}
		} else if (stat instanceof BlockStat) {
			openScope();
			for (Stat s : ((BlockStat)stat).getBlock().getStatements()) {
				translatStat(s);
			}
			closeScope();
		}
	}

	private void translatExpr(Expr e) {
		if (e instanceof KExpr) {
			translatKExpr((KExpr)e);
		} else if (e instanceof PrimaryExpr) {
			PrimaryExpr pe = (PrimaryExpr)e;
			Expr prefixExpr = pe.getPrefixExpr();
			if (prefixExpr instanceof Var) {
				translatVar((Var)prefixExpr);
			} else {
				translatExpr(e);
			}

		}
	}

	private void translatVar(Var var) {
		VarInfo info = varType(var.getName());
		if (info.type == VarInfo.LOCAL) {
			move(info.index);
		} else if (info.type == VarInfo.UPVALUE) {
			instruction(new Instruction(OpCode.GETUPVALUE, this.freeReg++, 0, 0));
		} else if (info.type == VarInfo.GLOBAL) {
			int idx = addKString(var.getName());
			instruction(new Instruction(OpCode.GETGLOBAL, this.freeReg++, idx));
		} else {
			assert(false);
		}

	}

	private void translatSegment(PrimaryExpr.Segment seg) {

	}

	private void translatKExpr(KExpr e) {
		if (e instanceof Nil) {
			loadNil();
		} else if (e instanceof True) {
			loadBoolean(true);
		} else if (e instanceof False) {
			loadBoolean(false);
		} else if (e instanceof LiteralNumber) {
			loadNumber(((LiteralNumber)e).getText());
		} else if (e instanceof LiteralString) {
			loadString(((LiteralString)e).getText());
		} else {
			assert(false);
		}
	}

	private void adjustAssign(int nargs, int nexps) {
		loadNil(this.freeReg, nargs - nexps);
	}

	private void adjustAssign(int nargs, int nexps, Expr expr) {
		int extra = nargs - nexps;
		translatExpr(expr);
		if (expr.hasMultRet()) {
		} else if (extra > 0){
			loadNil(this.freeReg, extra);
		}
	}

	private int reserveReg(int n) {
		return this.freeReg += n;
	}

	private void openScope() {
		this.scopes.add(new Scope());
	}

	private void closeScope() {
		int pc = this.code.size();
		int n = getLastScope().numOfLocVar;
		for (int i = 1; i <= n; ++i) {
			this.localVars.get(this.localVars.size() - i).endPC = pc;
		}
		int s = this.actVars.size();
		this.actVars.subList(s - n, s).clear();
		this.scopes.removeLast();
	}

	private Scope getLastScope() {
		return this.scopes.get(this.scopes.size() - 1);
	}

	private void addLocalVar(String name) {
		LocVar var = new LocVar(name);
		var.startPC = code.size();
		this.localVars.add(var);
		this.actVars.add(this.localVars.size() - 1);
		getLastScope().numOfLocVar++;
	}

	private int findLocalVar(String name) {
		for (int i = this.actVars.size() - 1; i >= 0; --i) {
			if (this.localVars.get(this.actVars.get(i)).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}

	private VarInfo varType(String name) {
		VarInfo info = VarInfo.singleton;
		Translator translator = this;
		int l = 0;
		while (translator != null) {
			info.index = translator.findLocalVar(name);
			if (info.index >= 0) {
				if (l == 0) {
					info.type = VarInfo.LOCAL;
				} else {
					info.type = VarInfo.UPVALUE;
				}
				return info;
			}
			l++;
			translator = translator.getParent();
		}

		info.type = VarInfo.GLOBAL;
		return info;
	}

	private int addKNumber(String n) {
		return addK(new LNumber(Double.parseDouble(n)));
	}

	private int addKString(String s) {
		return addK(new LString(s));
	}

	private int addK(LValue v) {
		assert(v instanceof LNumber || v instanceof LString);
		int i = findK(v);
		if (i >= 0) {
			return i;
		}

		this.ks.add(v);
		return this.ks.size() - 1;
	}

	private int findK(LValue v) {
		return this.ks.lastIndexOf(v);
	}

	private void move(int from) {
		instruction(new Instruction(OpCode.MOVE, this.freeReg++, from, 0));
	}

	private void loadNil() {
		loadNil(this.freeReg, 1);
	}

	private void loadNil(int from, int n) {
		this.freeReg += n;
		if (this.code.size() > 0) {
			Instruction prev = this.code.get(this.code.size() - 1);
			if (prev.getOpCode() == OpCode.LOADNIL && prev.getA() <= from && from <= prev.getB() + 1) {
				if (from + n - 1 > prev.getB()) {
					prev.setB(from + n - 1);
				}
				return;
			}
		}
		instruction(new Instruction(OpCode.LOADNIL, from, from + n - 1, 0));
	}

	private void loadBoolean(boolean v) {
		instruction(new Instruction(OpCode.LOADBOOL, this.freeReg++, v ? 1 : 0, 0));
	}

	private void loadString(String s) {
		loadK(new LString(s));
	}

	private void loadNumber(String n) {
		loadK(new LNumber(Double.parseDouble(n)));
	}

	private void loadK(LValue v) {
		int i = addK(v);
		instruction(new Instruction(OpCode.LOADK, this.freeReg++, i));
	}

	private Instruction instruction(Instruction inst) {
		this.code.add(inst);
		return this.code.get(this.code.size() - 1);
	}

	private class Scope {

		private int numOfLocVar;

	}

	private static class VarInfo {

		private static final int LOCAL = 0;
		private static final int UPVALUE = 1;
		private static final int GLOBAL = 2;

		private static final VarInfo singleton = new VarInfo();

		private int type;
		private int index;

	}

	private static class LocVar {

		private String name;
		private int startPC, endPC;

		private LocVar(String name) {
			this.name = name;
		}

		private Proto.LocVar toProtoLocVar() {
			return new Proto.LocVar(this.name, this.startPC, this.endPC);
		}

	}

}
