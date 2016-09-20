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

	enum RegAlloc {
		NEXT,
		ANY,
		RK,
		;
	}

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
			translatLocalStat((LocalStat)stat);
		} else if (stat instanceof BlockStat) {
			openScope();
			for (Stat s : ((BlockStat)stat).getBlock().getStatements()) {
				translatStat(s);
			}
			closeScope();
		} else if (stat instanceof FuncStat) {
			translatFuncStat((FuncStat)stat);
		} else if (stat instanceof FuncCallStat) {
			translatPrimaryExpr(((FuncCallStat)stat).getExpr(), RegAlloc.ANY);
		} else if (stat instanceof ReturnStat) {
			translatReturnStat((ReturnStat)stat);
		}
	}

	private void translatLocalStat(LocalStat stat) {
		String[] names = stat.getNames();
		for (String n : names) {
			addLocalVar(n);
		}
		Expr[] es = stat.getExprs();
		int extra = names.length;
		if (es != null) {
			extra -= es.length;
			Expr e = null;
			for (int i = 0; i < es.length; ++i) {
				e = es[i];
				translatExpr(e, RegAlloc.NEXT);
			}
			if (e.hasMultRet()) {
				Instruction last = ArrayUtil.get(getCode(), -1);
				setReturns(last, extra >= 0 ? extra + 1 : 0);
				reserveReg(extra);
				extra = 0;
			}
		}
		if (extra > 0) {
			loadNil(extra);
			reserveReg(extra);
		}
	}

	private void translatFuncStat(FuncStat stat) {
		int reg = translatFuncBody(stat.getBody());
		int nup = ArrayUtil.get(this.children, -1).upvalues.size();
		Instruction inst = ArrayUtil.get(getCode(), -(nup + 1));
		VarInfo info = singleVar(stat.getName().getVar().getName());
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
	}

	private void translatReturnStat(ReturnStat stat) {
		Expr[] es = stat.getExprs();
		int first = this.actVars.size(), nret = 0;
		if (es != null) {
			nret = es.length;
			Expr e = null;
			for (int i = 0; i < es.length; ++i) {
				e = es[i];
				translatExpr(e, RegAlloc.NEXT);
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

	private int translatExpr(Expr e, RegAlloc alloc) {
		int start = this.freeReg, result = -1;
		if (e instanceof KExpr) {
			result = translatKExpr((KExpr)e, alloc);
		} else if (e instanceof PrimaryExpr) {
			result = translatPrimaryExpr((PrimaryExpr)e, alloc);
		} else if (e instanceof FuncExpr) {
			FuncBody body = ((FuncExpr) e).getBody();
			result = translatFuncBody(body);
		} else if (e instanceof BinaryExpr) {

		} else if (e instanceof UnaryExpr) {

		} else if (e instanceof TableConstructorExpr) {

		} else if (e instanceof VarargExpr) {

		}

		checkRegAlloc(alloc, start, result);
		return result;
	}

	private int translatPrimaryExpr(PrimaryExpr expr, RegAlloc alloc) {
		int start = this.freeReg;

		Expr prefixExpr = expr.getPrefixExpr();
		RegAlloc prefixAlloc = expr.isVarExpr() ? alloc : RegAlloc.ANY;
		int reg;
		if (prefixExpr instanceof Var) {
			reg = translatVar((Var)prefixExpr, prefixAlloc);
		} else {
			reg = translatExpr(prefixExpr, prefixAlloc);
		}

		PrimaryExpr.Segment[] segs = expr.getSegments();
		if (ArrayUtil.isEmpty(segs)) {
			return reg;
		}

		int base = reg;
		if ((alloc == RegAlloc.NEXT && base != start) || reg != this.freeReg - 1) {
			assert(this.freeReg == start);
			base = reserveReg(1);
		}
		for (PrimaryExpr.Segment seg : segs) {
			if (seg instanceof PrimaryExpr.FieldSegment) {
				Expr key = ((PrimaryExpr.FieldSegment)seg).getKey();
				int rk = translatExpr(key, RegAlloc.RK);
				index(base, reg, rk);
			} else if (seg instanceof PrimaryExpr.FuncArgsSegment) {
				Expr[] args = ((PrimaryExpr.FuncArgsSegment)seg).getArgs();
				if (args != null) {
					for (Expr e : args) {
						translatExpr(e, RegAlloc.NEXT);
					}
				}
				int np = this.freeReg - base - 1;
				call(reg, np, 1);
				reserveReg(-np);
			} else if (seg instanceof PrimaryExpr.FieldAndArgsSegment) {

			}
		}

		return base;
	}

	private int translatVar(Var var, RegAlloc alloc) {
		VarInfo info = singleVar(var.getName());
		int reg = reserveReg(1);
		switch (info.type) {
			case VarInfo.LOCAL:
				if (alloc == RegAlloc.NEXT) {
					move(reg, info.index);
				} else {
					reserveReg(-1);
					reg = info.index;
				}
				break;
			case VarInfo.UPVALUE: {
				UpValue uv = this.upvalues.get(info.index);
				if (uv.inSameLevel) {
					move(reg, uv.index);
				} else {
					instruction(new Instruction(OpCode.GETUPVALUE, reg, uv.index, 0));
				}
				break;
			}
			case VarInfo.GLOBAL: {
				int idx = addKString(var.getName());
				instruction(new Instruction(OpCode.GETGLOBAL, reg, idx));
				break;
			}
			default:
				assert(false);
		}

		return reg;
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
		int reg = reserveReg(1);
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

	private int translatKExpr(KExpr e, RegAlloc alloc) {
		int i = addK(e.toLuaValue());
		if (alloc != RegAlloc.RK) {
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
			return this.freeReg - 1;
		}
		return Instruction.setAsK(i);
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
		int from = reserveReg(n);
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

	private void loadBoolean(boolean v){
		instruction(new Instruction(OpCode.LOADBOOL, reserveReg(1), v ? 1 : 0, 0));
	}

	private void loadK(int i) {
		instruction(new Instruction(OpCode.LOADK, reserveReg(1), i));
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

	private int reserveReg(int n) {
		int reg = this.freeReg;
		this.freeReg += n;
		return reg;
	}

	private void checkRegAlloc(RegAlloc alloc, int start, int result) {
		if (alloc == RegAlloc.NEXT) {
			assert(isNext(start, result));
		} else if (alloc == RegAlloc.ANY) {
			assert(isAny(start, result));
		} else if (alloc == RegAlloc.RK) {
			assert(isK(start, result));
		}
	}

	private boolean isNext(int start, int result) {
		return result == start && this.freeReg - result == 1;
	}

	private boolean isAny(int start, int result) {
		return (result < start && this.freeReg == start) || isNext(start, result);
	}

	private boolean isK(int start, int result) {
		return Instruction.isK(result) || isAny(start, result);
	}

}
