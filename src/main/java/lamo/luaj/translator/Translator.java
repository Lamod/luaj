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

	static public final int RA_NEXT = -1;
	static public final int RA_ANY = -2;
	static public final int RA_RK = -3;

	private Chunk chunk;
	private int numParams;
	private boolean vararg;
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

	public Proto translate() {
		if (this.proto != null) {
			return this.proto;
		}

		openScope();
		Stat[] stats = this.chunk.getStatements();
		if (!ArrayUtil.isEmpty(stats)) {
			for (Stat stat : this.chunk.getStatements()) {
				translateStat(stat);
				this.freeReg = this.actVars.size();
			}
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

		this.proto = new Proto(is, ps, ls, null, ks, this.numParams, this.vararg);
		return this.proto;
	}

	public String toString() {
		return this.proto.toString();
	}

	private void translateStat(Stat stat) {
		if (stat instanceof LocalStat) {
			translateLocalStat((LocalStat)stat);
		} else if (stat instanceof BlockStat) {
			openScope();
			for (Stat s : ((BlockStat)stat).getBlock().getStatements()) {
				translateStat(s);
			}
			closeScope();
		} else if (stat instanceof FuncStat) {
			translateFuncStat((FuncStat)stat);
		} else if (stat instanceof FuncCallStat) {
			translatePrimaryExpr(((FuncCallStat)stat).getExpr(), RA_ANY);
		} else if (stat instanceof ReturnStat) {
			translateReturnStat((ReturnStat)stat);
		}
	}

	private void translateLocalStat(LocalStat stat) {
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
				translateExpr(e, RA_NEXT);
			}
			if (e.hasMultRet()) {
				Instruction last = ArrayUtil.get(getCode(), -1);
				setReturns(last, extra >= 0 ? extra + 1 : 0);
				reserveReg(extra);
				extra = 0;
			}
		}
		if (extra > 0) {
			loadNil(reserveReg(extra), extra);
		}
	}

	private void translateFuncStat(FuncStat stat) {
		VarInfo info = singleVar(stat.getName().getVar().getName());
		switch (info.type) {
			case VarInfo.LOCAL:
				translateFuncBody(stat.getBody(), info.index);
				break;
			case VarInfo.UPVALUE: {
				int reg = translateFuncBody(stat.getBody(), RA_ANY);
				instruction(new Instruction(OpCode.SETUPVALUE, info.index, reg, 0));
				break;
			}
			case VarInfo.GLOBAL: {
				int reg = translateFuncBody(stat.getBody(), RA_ANY);
				instruction(new Instruction(OpCode.SETGLOBAL, info.index, reg));
				break;
			}
			default:
				assert(false);
		}
	}

	private void translateReturnStat(ReturnStat stat) {
		Expr[] es = stat.getExprs();
		int first = this.actVars.size(), nret = 0;
		if (es != null) {
			nret = es.length;
			Expr e = null;
			for (int i = 0; i < es.length; ++i) {
				e = es[i];
				translateExpr(e, RA_NEXT);
			}
			if (e.hasMultRet()) {
				Instruction last = ArrayUtil.get(getCode(), -1);
				setReturns(last, -1);
				if (last.getOpCode() == OpCode.CALL && es.length == 1) {
					last.setOpCode(OpCode.TAILCALL);
				}
				nret = -1;
			}
		}
		ret(first, nret);
	}

	private int translateExpr(Expr e, int alloc) {
		int start = this.freeReg, result = -1;
		if (e instanceof KExpr) {
			result = translateKExpr((KExpr)e, alloc);
		} else if (e instanceof PrimaryExpr) {
			result = translatePrimaryExpr((PrimaryExpr)e, alloc);
		} else if (e instanceof FuncExpr) {
			FuncBody body = ((FuncExpr) e).getBody();
			result = translateFuncBody(body, alloc);
		} else if (e instanceof VarargExpr) {
			assert(alloc == RA_NEXT);
			result = reserveReg(1);
			instruction(new Instruction(OpCode.VARARG, result, 2, 0));
		} else if (e instanceof BinaryExpr) {

		} else if (e instanceof UnaryExpr) {

		} else if (e instanceof TableConstructorExpr) {

		} else {
			assert(false);
		}

		checkRegAlloc(alloc, start, result);
		return result;
	}

	private int translatePrimaryExpr(PrimaryExpr expr, int alloc) {
		int start = this.freeReg;

		int prefixAlloc;
		if (expr.isVarExpr()) {
			prefixAlloc = alloc;
		} else if (expr.getSegments()[0] instanceof PrimaryExpr.FuncArgsSegment) {
			prefixAlloc = RA_NEXT;
		} else {
			prefixAlloc = RA_ANY;
		}
		Expr prefixExpr = expr.getPrefixExpr();
		int table;
		if (prefixExpr instanceof Var) {
			table = translateVar((Var)prefixExpr, prefixAlloc);
		} else {
			table = translateExpr(prefixExpr, prefixAlloc);
		}
		if (expr.isVarExpr()) {
			return table;
		}

		int base = table;
		if (base != start) {
			assert(this.freeReg == start);
			base = reserveReg(1);
		}
		for (PrimaryExpr.Segment seg : expr.getSegments()) {
			if (seg instanceof PrimaryExpr.FieldSegment) {
				Expr key = ((PrimaryExpr.FieldSegment)seg).getKey();
				int rk = translateExpr(key, RA_RK);
				index(base, table, rk);
			} else if (seg instanceof PrimaryExpr.FuncArgsSegment) {
				Expr[] args = ((PrimaryExpr.FuncArgsSegment)seg).getArgs();
				if (args != null) {
					for (Expr e : args) {
						translateExpr(e, RA_NEXT);
					}
				}
				int np = this.freeReg - base - 1;
				call(base, np, 1);
				reserveReg(-np);
			}
		}

		if (alloc >= 0) {
			assert(alloc < start);
			if (expr.isIndexExpr()) {
				Instruction last = ArrayUtil.get(this.getCode(), -1);
				assert(last.getOpCode() == OpCode.GETTABLE);
				last.setA(alloc);
			} else {
				move(alloc, base);
			}
			reserveReg(-1);
			return alloc;
		} else {
			return base;
		}
	}

	private int translateVar(Var var, int alloc) {
		VarInfo info = singleVar(var.getName());
		int reg = alloc >= 0 ? alloc : reserveReg(1);
		switch (info.type) {
			case VarInfo.LOCAL:
				if (alloc >= RA_NEXT) {
					move(reg, info.index);
				}
				if (alloc != RA_NEXT){
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

	private int translateFuncBody(FuncBody body, int alloc) {
		Translator t = new Translator(body.getChunk(), this);
		t.vararg = body.getParlist().isVararg();
		if (body.isNeedSelf()) {
			t.numParams++;
			t.addLocalVar("self");
		}
		if (body.getParlist() != null && body.getParlist().getParams() != null) {
			for (String p : body.getParlist().getParams()) {
				t.numParams++;
				t.addLocalVar(p);
			}
		}

		Proto p = t.translate();
		int reg = alloc >= 0 ? alloc : reserveReg(1);
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

	private int translateKExpr(KExpr e, int alloc) {
		int i = addK(e.toLuaValue());
		if (alloc == RA_RK) {
			return Instruction.setAsK(i);
		}

		int reg = alloc >= 0 ? alloc : reserveReg(1);
		if (e instanceof Nil) {
			loadNil(reg);
		} else if (e instanceof True) {
			loadBoolean(true, reg);
		} else if (e instanceof False) {
			loadBoolean(false, reg);
		} else if (e instanceof LiteralNumber) {
			loadK(i, reg);
		} else if (e instanceof LiteralString) {
			loadK(i, reg);
		} else {
			assert(false);
		}
		return reg;
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
		} else if (inst.getOpCode() == OpCode.VARARG) {
			inst.setB(nret + 1);
		}
		if (nret >= 0) {
			reserveReg(nret - 1);
		}
	}

	private void move(int to, int from) {
		if (from != to) {
			instruction(new Instruction(OpCode.MOVE, to, from, 0));
		}
	}

	private void loadNil(int reg) {
		loadNil(reg, 1);
	}

	private void loadNil(int from, int n) {
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

	private void loadBoolean(boolean v, int reg){
		instruction(new Instruction(OpCode.LOADBOOL, reg, v ? 1 : 0, 0));
	}

	private void loadK(int i, int reg) {
		instruction(new Instruction(OpCode.LOADK, reg, i));
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

	private void checkRegAlloc(int alloc, int start, int result) {
		if (alloc >= 0) {
			assert(alloc == result && this.freeReg == start);
		} else if (alloc == RA_NEXT) {
			assert(isNext(start, result));
		} else if (alloc == RA_ANY) {
			assert(isAny(start, result));
		} else if (alloc == RA_RK) {
			assert(isK(start, result));
		} else {
			assert(false);
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
