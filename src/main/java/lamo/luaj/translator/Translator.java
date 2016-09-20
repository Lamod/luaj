package lamo.luaj.translator;

import lamo.luaj.LNumber;
import lamo.luaj.LString;
import lamo.luaj.LValue;
import lamo.luaj.parser.ast.*;
import lamo.luaj.util.ArrayUtil;
import lamo.luaj.vm.Instruction;
import lamo.luaj.vm.OpCode;
import lamo.luaj.vm.Proto;

import java.util.ArrayList;
import java.util.LinkedList;

public class Translator {

	private Chunk chunk;
	private Proto proto;
	private Translator parent;
	private ArrayList<Translator> children = new ArrayList<>();

	private ArrayList<Instruction> code = new ArrayList<>();
	private ArrayList<LocVar> localVars = new ArrayList<>();
	private LinkedList<Integer> actVars = new LinkedList<>();
	private ArrayList<UpValue> upvalues = new ArrayList<>();
	private ArrayList<LValue> ks = new ArrayList<>();
	private int freeReg;

	private LinkedList<Scope> scopes = new LinkedList<>();
	private ArrayList<Proto> ps = new ArrayList<>();

	public Translator(Chunk chunk) {
		this(chunk, null);
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

	public Proto getProto() {
		return this.proto;
	}

	public Translator getParent() {
		return this.parent;
	}

	public Translator[] getChildren() {
		return this.children.toArray(new Translator[this.children.size()]);
	}

	public Proto translat() {
		if (this.proto != null) {
			return this.proto;
		}

		openScope();
		for (Stat stat : this.chunk.getStatements()) {
			translatStat(stat);
			this.freeReg = this.actVars.size();
		}
		ret(0, 0);
		closeScope();

		ArrayList<Instruction> code = getCode();
		Instruction[] is = code.toArray(new Instruction[code.size()]);
		Proto[] ps = this.ps.toArray(new Proto[this.ps.size()]);
		Proto.LocVar[] ls = new Proto.LocVar[this.localVars.size()];
		for (int i = 0; i < this.localVars.size(); ++i) {
			ls[i] = this.localVars.get(i).toProtoLocVar();
		}
		String[] uvs = new String[this.upvalues.size()];
		for (int i = 0; i < this.upvalues.size(); ++i) {
			uvs[i] = this.upvalues.get(i).name;
		}
		LValue[] ks = this.ks.toArray(new LValue[this.ks.size()]);

		this.proto = new Proto(is, ps, ls, null, ks);
		return this.proto;
	}

	public String toString() {
		return this.proto.toString();
	}

	private void translatStat(Stat stat) {
		if (stat instanceof LocalStat) {
			LocalStat ls = (LocalStat)stat;
			String[] names = ls.getNames();
			for (String n : names) {
				addLocalVar(n);
			}
			Expr[] es = ls.getExprs();
			int extra = names.length;
			if (es != null) {
				extra -= es.length;
				Expr e = null;
				for (int i = 0; i < es.length; ++i) {
					e = es[i];
					translatExpr(e);
				}
				if (e.hasMultRet()) {
					Instruction last = ArrayUtil.get(getCode(), -1);
					setReturns(last, extra >= 0 ? extra + 1 : 0);
					extra = 0;
				}
			}
			if (extra > 0) {
				loadNil(extra);
			}
		} else if (stat instanceof BlockStat) {
			openScope();
			for (Stat s : ((BlockStat)stat).getBlock().getStatements()) {
				translatStat(s);
			}
			closeScope();
		} else if (stat instanceof FuncStat) {
			FuncStat fs = (FuncStat)stat;
			int reg = translatFuncBody(fs.getBody());
			int nup = ArrayUtil.get(this.children, -1).upvalues.size();
			Instruction inst = ArrayUtil.get(getCode(), -(nup + 1));
			VarInfo info = translatVar(fs.getName().getVar());
			switch (info.type) {
				case VarInfo.LOCAL:
					inst.setA(info.index);
					break;
				case VarInfo.UPVALUE:
					instruction(new Instruction(OpCode.SETUPVALUE, info.index, reg, 0));
					break;
				case VarInfo.GLOBAL:
					instruction(new Instruction(OpCode.SETGLOBAL, info.index, reg));
					break;
				default:
					assert(false);
			}
		} else if (stat instanceof FuncCallStat) {
			translatPrimaryExpr(((FuncCallStat)stat).getExpr());
		} else if (stat instanceof ReturnStat) {
			Expr[] es = ((ReturnStat)stat).getExprs();
			int first = this.actVars.size(), nret = 0;
			if (es != null) {
				nret = es.length;
				Expr e = null;
				for (int i = 0; i < es.length; ++i) {
					e = es[i];
					translatExpr(e);
				}
				if (e.hasMultRet()) {
					Instruction last = ArrayUtil.get(getCode(), -1);
					if (last.getOpCode() == OpCode.CALL) {
						setReturns(last, -1);
						if (es.length == 1) {
							last.setOpCode(OpCode.TAILCALL);
						}
					}
					nret = -1;
				}
			}
			ret(first, nret);
		}
	}

	private int translatFuncBody(FuncBody body) {
		Translator t = new Translator(body.getChunk(), this);
		if (body.isNeedSelf()) {
			t.addLocalVar("self");
		}
		if (body.getParlist() != null && body.getParlist().getParams() != null) {
			for (String p : body.getParlist().getParams()) {
				t.addLocalVar(p);
			}
		}

		Proto p = t.translat();
		int reg = this.freeReg++;
		this.ps.add(p);
		instruction(new Instruction(OpCode.CLOSURE, reg, this.ps.size() - 1));
		UpValue uv;
		for (int i = 0; i < t.upvalues.size(); ++i) {
			uv = t.upvalues.get(i);
			if (uv.inSameLevel) {
				instruction(new Instruction(OpCode.MOVE, i, uv.index, 0));
			} else {
				instruction(new Instruction(OpCode.GETUPVALUE, i, uv.index, 0));
			}
		}

		return reg;
	}

	private int translatRK(Expr e) {
		if (e instanceof KExpr) {
			return Instruction.setAsK(addK(((KExpr) e).toLuaValue()));
		} else {
			return translatExpr(e);
		}
	}

	private int translatExpr(Expr e) {
		if (e instanceof KExpr) {
			translatKExpr((KExpr)e);
		} else if (e instanceof PrimaryExpr) {
			translatPrimaryExpr((PrimaryExpr)e);
		} else if (e instanceof FuncExpr) {
			FuncBody body = ((FuncExpr) e).getBody();
			translatFuncBody(body);
		} else if (e instanceof BinaryExpr) {

		} else if (e instanceof UnaryExpr) {

		} else if (e instanceof TableConstructorExpr) {

		} else if (e instanceof VarargExpr) {

		}
		return this.freeReg - 1;
	}

	private VarInfo translatVar(Var var) {
		return singleVar(var.getName());
	}

	private void translatPrimaryExpr(PrimaryExpr expr) {
		Expr prefixExpr = expr.getPrefixExpr();
		int base;
		if (prefixExpr instanceof Var) {
			base = this.freeReg++;
			Var var = (Var)prefixExpr;
			VarInfo info = translatVar(var);
			switch (info.type) {
				case VarInfo.LOCAL:
					move(base, info.index);
					break;
				case VarInfo.UPVALUE: {
					UpValue uv = this.upvalues.get(info.index);
					if (uv.inSameLevel) {
						move(base, uv.index);
					} else {
						instruction(new Instruction(OpCode.GETUPVALUE, base, uv.index, 0));
					}
					break;
				}
				case VarInfo.GLOBAL: {
					int idx = addKString(var.getName());
					instruction(new Instruction(OpCode.GETGLOBAL, base, idx));
					break;
				}
				default:
					assert(false);
			}
		} else {
			base = translatExpr(prefixExpr);
		}
		PrimaryExpr.Segment[] segs = expr.getSegments();
		if (segs != null) {
			for (PrimaryExpr.Segment seg : segs) {
				translatSegment(base, seg);
				this.freeReg = base + 1;
			}
		}
	}

	private void translatSegment(int base, PrimaryExpr.Segment seg) {
		if (seg instanceof PrimaryExpr.FieldSegment) {
			Expr key = ((PrimaryExpr.FieldSegment)seg).getKey();
			int rk = translatRK(key);
			index(base, base, rk);
		} else if (seg instanceof PrimaryExpr.FuncArgsSegment) {
			Expr[] args = ((PrimaryExpr.FuncArgsSegment)seg).getArgs();
			if (args != null) {
				for (Expr e : args) {
					translatExpr(e);
				}
			}
			int np = this.freeReg - base - 1;
			call(base, np, 1);
		} else if (seg instanceof PrimaryExpr.FieldAndArgsSegment) {

		}
	}

	private void translatKExpr(KExpr e) {
		int i = addK(e.toLuaValue());
		if (e instanceof Nil) {
			loadNil();
		} else if (e instanceof True) {
			loadBoolean(true);
		} else if (e instanceof False) {
			loadBoolean(false);
		} else if (e instanceof LiteralNumber) {
			loadK(i);
		} else if (e instanceof LiteralString) {
			loadK(i);
		} else {
			assert(false);
		}
	}

	private void openScope() {
		this.scopes.add(new Scope());
	}

	private void closeScope() {
		int pc = getCode().size();
		int n = getLastScope().numOfLocVar;
		for (int i = 1; i <= n; ++i) {
			this.localVars.get(this.localVars.size() - i).endPC = pc;
		}
		int s = this.actVars.size();
		this.actVars.subList(s - n, s).clear();
		this.scopes.removeLast();
	}

	private Scope getLastScope() {
		return this.scopes.getLast();
	}

	private int addLocalVar(String name) {
		LocVar var = new LocVar(name);
		var.startPC = getCode().size();
		this.localVars.add(var);
		this.actVars.add(this.localVars.size() - 1);
		return getLastScope().numOfLocVar++;
	}

	private int findLocalVar(String name) {
		for (int i = this.actVars.size() - 1; i >= 0; --i) {
			if (this.localVars.get(this.actVars.get(i)).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}

	private int addUpValue(String name, int idx, boolean inSameChunk) {
		int index = findUpValue(name);
		if (index >= 0) {
			return index;
		}

		UpValue uv = new UpValue();
		uv.name = name;
		uv.index = idx;
		uv.inSameLevel = inSameChunk;
		this.upvalues.add(uv);
		return this.upvalues.size() - 1;
	}

	private int findUpValue(String name) {
		for (int i = 0; i < this.upvalues.size(); ++i) {
			if (this.upvalues.get(i).name.equals(name)) {
				return i;
			}
		}
		return -1;
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

	private VarInfo singleVar(String name) {
		VarInfo info = VarInfo.singleton;
		info.type = VarInfo.GLOBAL;
		ArrayList<Translator> ts = new ArrayList<>();
		Translator t = this, last = null;
		do {
			info.index = t.findLocalVar(name);
			if (info.index >= 0) {
				if (this == t) {
					info.type = VarInfo.LOCAL;
				} else {
					info.type = VarInfo.UPVALUE;
					int idx = 0;
					for (Translator tr : ts) {
						if (tr == last) {
							idx = tr.addUpValue(name, info.index, true);
						} else {
							idx = tr.addUpValue(name, idx, false);
						}
						if (tr == this) {
							info.index = idx;
						}
					}
				}
				return info;
			} else if (this == t && (info.index = t.findUpValue(name)) >= 0) {
				info.type = VarInfo.UPVALUE;
				return info;
			}
			last = t;
			ts.add(t);
		} while ((t = t.getParent()) != null);

		return info;
	}

	private void index(int to, int table, int key) {
		instruction(new Instruction(OpCode.GETTABLE, to, table, key));
	}

	private void call(int base, int nparam, int nret) {
		instruction(new Instruction(OpCode.CALL, base, nparam + 1, nret + 1));
	}

	private void ret(int first, int nret) {
		instruction(new Instruction(OpCode.RETURN, first, nret + 1, 0));
	}

	private void setReturns(Instruction inst, int nret) {
		if (inst.getOpCode() == OpCode.CALL) {
			assert(nret >= -1);
			inst.setC(nret + 1);
		}
	}

	private void move(int to, int from) {
		if (from != to) {
			instruction(new Instruction(OpCode.MOVE, to, from, 0));
		}
	}

	private void loadNil() {
		loadNil(1);
	}

	private void loadNil(int n) {
		int from = this.freeReg;
		this.freeReg += n;
		ArrayList<Instruction> code = getCode();
		if (code.size() > 0) {
			Instruction prev = ArrayUtil.get(code, -1);
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

	private void loadK(int i) {
		instruction(new Instruction(OpCode.LOADK, this.freeReg++, i));
	}

	private void instruction(Instruction inst) {
		if (inst == null) {
			return;
		}
		getCode().add(inst);
	}

	private ArrayList<Instruction> getCode() {
		return this.code;
	}

}
