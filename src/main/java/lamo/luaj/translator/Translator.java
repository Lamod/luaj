package lamo.luaj.translator;

import lamo.luaj.*;
import lamo.luaj.parser.ast.*;
import lamo.luaj.util.ArrayUtil;
import lamo.luaj.util.BoolUtil;
import lamo.luaj.vm.Instruction;
import lamo.luaj.vm.OpCode;
import lamo.luaj.vm.Proto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Translator {

	static public final int RA_NEXT = -1;
	static public final int RA_ANY = -2;
	static public final int RA_RK = -3;
	static public final int RA_NONE = -4;

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
		if (stat instanceof IfStat) {
			translateIfStat((IfStat)stat);
		} else if (stat instanceof LocalStat) {
			translateLocalStat((LocalStat)stat);
		} else if (stat instanceof BlockStat) {
			block(((BlockStat)stat).getBlock());
		} else if (stat instanceof FuncStat) {
			translateFuncStat((FuncStat)stat);
		} else if (stat instanceof FuncCallStat) {
			PrimaryExpr pe = ((FuncCallStat)stat).getExpr();
			translatePrimaryExpr(pe, RA_ANY);
			if (pe.isFuncCallExpr()) {
				Instruction last = ArrayUtil.get(getCode(), -1);
				setReturns(last, 0);
			}
		} else if (stat instanceof ReturnStat) {
			translateReturnStat((ReturnStat)stat);
		} else if (stat instanceof AssignStat) {
			translateAssignStat((AssignStat)stat);
		}
	}

	private void translateIfStat(IfStat stat) {
		ArrayList<Integer> elseList = null, endList = new ArrayList<>();
		boolean first = true;
		for (IfStat.Branch branch : stat.getBranchList()) {
			if (!first) {
				instruction(new Instruction(OpCode.JMP, 0, Instruction.NO_JUMP));
				endList.add(pc() - 1);
				patchToHere(elseList);
			} else {
				first = false;
			}
			if (branch.isElseBranch()) {
				block(branch.getBlock());
			} else {
				elseList = testThenBlock(branch.getCondition(), branch.getBlock());
			}
		}

		patchToHere(endList);
		patchToHere(elseList);
	}

	private ArrayList<Integer> testThenBlock(Expr cond, Block block) {
		LogicalContext ctx = logicalLeftOperand(cond, true);
		patchToHere(ctx.tlist);
		block(block);

		return ctx.flist;
	}

	private void block(Block block) {
		openScope();
		for (Stat s : block.getStatements()) {
			translateStat(s);
		}
		closeScope();
	}

	private void translateLocalStat(LocalStat stat) {
		String[] names = stat.getNames();
		Expr[] es = stat.getExprs();
		translateAssignValues(es, names.length);

		for (String n : names) {
			addLocalVar(n);
		}
	}

	private void translateFuncStat(FuncStat stat) {
		PrimaryExpr pe = new PrimaryExpr();

		Var var = stat.getName().getVar();
		pe.setPrefixExpr(var);

		String[] fields = stat.getName().getFields();
		ArrayUtil.Mapper<PrimaryExpr.Segment, String> mapper =
		new ArrayUtil.Mapper<PrimaryExpr.Segment, String>() {
			@Override
			public PrimaryExpr.Segment map(String s) {
				PrimaryExpr.FieldSegment fs = new PrimaryExpr.FieldSegment();
				fs.setKey(new LiteralString(s));
				return fs;
			}
		};
		PrimaryExpr.Segment[] segs = ArrayUtil.map(fields, mapper, new PrimaryExpr.Segment[fields.length]);
		pe.setSegments(segs);

		AssignVarInfo info = translateAssignVar(pe);
		int reg = translateFuncBody(stat.getBody(), RA_NEXT);
		storeVar(info, reg);
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

	private void translateAssignStat(AssignStat stat) {
		PrimaryExpr[] vars = stat.getVariables();
		Expr[] vs = stat.getValues();

		ArrayList<AssignVarInfo> infos = new ArrayList<>();
		for (PrimaryExpr var : vars) {
			infos.add(translateAssignVar(var));
		}

		int start = this.freeReg;
		translateAssignValues(vs, vars.length);
		for (int i = infos.size() - 1; i >= 0; --i) {
			storeVar(infos.get(i), start + i);
		}
	}

	private AssignVarInfo translateAssignVar(PrimaryExpr var) {
		Expr prefix = var.getPrefixExpr();
		PrimaryExpr.Segment[] segments = var.getSegments();

		if (!ArrayUtil.isEmpty(segments)) {
			PrimaryExpr pe = new PrimaryExpr();
			pe.setPrefixExpr(prefix);
			if (segments.length > 1) {
				pe.setSegments(Arrays.copyOfRange(segments, 0, segments.length - 1));
			}
			int table = translatePrimaryExpr(pe, RA_NEXT);
			Expr key = ((PrimaryExpr.FieldSegment)ArrayUtil.get(segments, -1)).getKey();
			return AssignVarInfo.table(table, key);
		} else if (prefix instanceof Var) {
			VarInfo info = singleVar(((Var)prefix).getName());
			if (info.type == VarInfo.LOCAL) {
				return AssignVarInfo.local(info.index);
			} else if (info.type == VarInfo.UPVALUE) {
				return AssignVarInfo.upvalue(info.index);
			} else if (info.type == VarInfo.GLOBAL) {
				return AssignVarInfo.global(info.index);
			} else {
				assert false;
				return null;
			}
		} else if (prefix instanceof PrimaryExpr) {
			return translateAssignVar((PrimaryExpr)prefix);
		} else {
			assert false;
			return null;
		}
	}

	private int translateAssignValues(Expr[] vs, int need) {
		int extra = need;
		if (vs != null) {
			extra -= vs.length;
			Expr v = null;
			for (int i = 0; i < vs.length; ++i) {
				v = vs[i];
				translateExpr(v, RA_NEXT);
			}
			if (v.hasMultRet()) {
				Instruction last = ArrayUtil.get(getCode(), -1);
				setReturns(last, extra >= 0 ? extra + 1 : 0);
				reserveReg(extra);
				extra = 0;
			}
		}
		if (extra > 0) {
			loadNil(reserveReg(extra), extra);
		}

		return this.freeReg;
	}

	private int translateExpr(Expr e, int alloc) {
		e = ExprUtil.reduce(e);
		int start = this.freeReg, result = Instruction.NO_REG;
		if (e instanceof KExpr) {
			result = translateKExpr((KExpr) e, alloc);
		} else if (e instanceof Var) {
			result = translateVar((Var)e, alloc);
		} else if (e instanceof PrimaryExpr) {
			result = translatePrimaryExpr((PrimaryExpr)e, alloc);
		} else if (e instanceof FuncExpr) {
			FuncBody body = ((FuncExpr) e).getBody();
			result = translateFuncBody(body, alloc);
		} else if (e instanceof VarargExpr) {
			result = reserveReg(1);
			instruction(new Instruction(OpCode.VARARG, result, 2, 0));
		} else if (e instanceof BinaryExpr) {
			result = translateBinaryExpr((BinaryExpr)e, alloc);
		} else if (e instanceof UnaryExpr) {
			result = translateUnaryExpr((UnaryExpr)e, alloc);
		} else if (e instanceof TableConstructorExpr) {
			result = translateTableConstructor((TableConstructorExpr)e, alloc);
		} else {
			assert false;
		}

		checkRegAlloc(alloc, start, result);
		return result;
	}

	private int translateTableConstructor(TableConstructorExpr expr, int alloc) {
		int table = reserveReg(1);
		Instruction inst = new Instruction(OpCode.NEWTABLE, table, 0, 0);
		instruction(inst);

		TableConstructorExpr.Field[] fs = expr.getFields();
		if (!ArrayUtil.isEmpty(fs)) {
			int na = 0, toStore = 0, nh = 0;
			TableConstructorExpr.Field f;
			for (int i = 0; i < fs.length; ++i) {
				f = fs[i];
				if (f instanceof TableConstructorExpr.ListField) {
					na++;
					toStore++;
					translateExpr(f.getValue(), RA_NEXT);
				} else {
					nh++;
					TableConstructorExpr.RecField rf = (TableConstructorExpr.RecField)f;
					int reg = this.freeReg;
					int k = translateExpr(rf.getKey(), RA_RK);
					int v = translateExpr(rf.getValue(), RA_RK);
					instruction(new Instruction(OpCode.SETTABLE, table, k, v));
					if (reg != this.freeReg) {
						reserveReg(reg - this.freeReg);
					}
				}
				if (toStore > 0 && (toStore == Config.LFIELDS_PER_FLUSH || i == fs.length - 1)) {
					int b;
					if (i == fs.length - 1
							&& f instanceof TableConstructorExpr.ListField
							&& f.getValue().hasMultRet()) {
						Instruction last = ArrayUtil.get(getCode(), -1);
						setReturns(last, -1);
						na--;
						b = 0;
					} else {
						b = toStore;
					}
					int c = (na - 1) / Config.LFIELDS_PER_FLUSH + 1;
					if (c <= Instruction.MAX_C) {
						instruction(new Instruction(OpCode.SETLIST, table, b, c));
					} else {
						instruction(new Instruction(OpCode.SETLIST, table, b, 0));
						instruction(new Instruction(c));
					}
					reserveReg(-toStore);
					toStore = 0;
				}
			}
			inst.setB(na);
			inst.setC(nh);
		}

		if (alloc >= 0) {
			move(alloc, table);
			reserveReg(-1);
			return alloc;
		} else {
			return table;
		}
	}

	private int translateBinaryExpr(BinaryExpr expr, int alloc) {
		if (ExprUtil.isArithExpr(expr)) {
			return translateArithExpr(expr, alloc);
		} else if (ExprUtil.isCompExpr(expr)) {
			return translateCompExpr(expr, alloc);
		} else {
			assert ExprUtil.isLogicalExpr(expr);
			return translateLogicalExpr(expr, alloc);
		}
	}

	private int translateLogicalExpr(BinaryExpr expr, int alloc) {
		if (alloc < RA_NEXT) {
			alloc = RA_NEXT;
		}
		LogicalContext ctx = logicalOperand(expr, true, alloc);
		return closeLogicalContext(ctx, alloc);
	}

	private int closeLogicalContext(LogicalContext ctx, int alloc) {
		int pc, pf = Instruction.NO_JUMP, pt = Instruction.NO_JUMP;
		if (alloc == RA_NONE) {
			pc = pc();
			ctx.reg = Instruction.NO_REG;
		} else if (ctx.needValue) {
			if (ctx.reg == Instruction.NO_REG) {
				ctx.reg = requestReg(alloc);
			}
			pt = compValue(ctx.reg);
			pc = pt + 1;
			pf = pt - 1;
		} else {
			pc = pc();
		}
		patchList(ctx.flist, pc, pf, ctx.reg);
		patchList(ctx.tlist, pc, pt, ctx.reg);

		return ctx.reg;
	}

	private LogicalContext logicalLeftOperand(Expr expr, boolean goIfTrue) {
		expr = ExprUtil.reduce(expr);
		if (ExprUtil.isLogicalExpr(expr)) {
			return logicalOperand((BinaryExpr)expr, goIfTrue, RA_NONE);
		} else if (ExprUtil.isCompExpr(expr)) {
			return logicalCompOperand((BinaryExpr)expr, goIfTrue);
		} else if (expr instanceof LiteralNumber
				|| expr instanceof LiteralString
				|| expr instanceof True) {
			return new LogicalContext();
		} else {
			return logicalNormalOperand(expr, goIfTrue);
		}
	}

	private LogicalContext logicalRightOperand(Expr expr, boolean goIfTrue, int alloc) {
		expr = ExprUtil.reduce(expr);
		if (ExprUtil.isLogicalExpr(expr)) {
			return logicalOperand((BinaryExpr)expr, goIfTrue, alloc);
		} else if (ExprUtil.isCompExpr(expr)) {
			return logicalCompOperand((BinaryExpr)expr, alloc == RA_NONE && goIfTrue);
		} else if (expr instanceof Nil || expr instanceof False) {
			return new LogicalContext();
		} else {
			if (alloc == RA_NONE) {
				return logicalNormalOperand(expr, goIfTrue);
			} else {
				return new LogicalContext(translateExpr(expr, alloc));
			}
		}
	}

	private LogicalContext logicalOperand(BinaryExpr expr, boolean goIfTrue, int alloc) {
		boolean and = expr.getOperator() == BinaryExpr.Operator.AND;
		LogicalContext left = logicalLeftOperand(expr.getLeft(), and);
		patchToHere(left.get(and));

		LogicalContext right = logicalRightOperand(expr.getRight(), goIfTrue, alloc);
		right.merge(left);

		return right;
	}

	private LogicalContext logicalCompOperand(BinaryExpr expr, boolean goIfTrue) {
		LogicalContext ctx = new LogicalContext(translateCompExpr(expr, RA_NONE));
		if (goIfTrue) {
			Instruction inst = ArrayUtil.get(getCode(), -2);
			inst.setA(inst.getA() == 0);
		}
		ctx.add(pc() - 1, !goIfTrue);
		ctx.needValue = true;

		return ctx;
	}

	private LogicalContext logicalNormalOperand(Expr expr, boolean goIfTrue) {
		int reg = translateExpr(expr, RA_ANY);
		freeReg(reg);

		LogicalContext ctx = new LogicalContext(reg);
		int jmp;
		if (expr instanceof UnaryExpr
				&& ((UnaryExpr)expr).getOperator() == UnaryExpr.Operator.NOT) {
			ctx.needValue = true;
			int not = pc() - 1;
			int a = getCode().get(not).getB();
			getCode().remove(not);
			jmp = condJump(OpCode.TEST, a, 0, BoolUtil.toInt(goIfTrue));
		} else {
			jmp = condJump(OpCode.TESTSET, Instruction.NO_REG, reg, BoolUtil.invert(goIfTrue));
		}
		ctx.add(jmp, !goIfTrue);

		return ctx;
	}

	private int translateCompExpr(BinaryExpr expr, int alloc) {
		BinaryExpr.Operator operator = expr.getOperator();
		OpCode op = ExprUtil.toCompOpCode(operator);
		assert op != null;

		int cond;
		if (operator == BinaryExpr.Operator.NOT_EQUAL
				|| operator == BinaryExpr.Operator.GREATE_THAN
				|| operator == BinaryExpr.Operator.GREATE_EQUAL) {
			cond = 0;
		} else {
			cond = 1;
		}

		int start = this.freeReg;
		int left = translateExpr(expr.getLeft(), RA_RK), right = translateExpr(expr.getRight(), RA_RK);
		this.freeReg = start;

		if (cond == 0 && op != OpCode.EQ) {
			int temp = left;
			left = right;
			right = temp;
			cond = 1;
		}
		Instruction jmp = getCode().get(condJump(op, cond, left, right));
		if (alloc != RA_NONE) {
			int reg = requestReg(alloc);
			jmp.setBx(1);
			compValue(reg);
			return reg;
		}

		return Instruction.NO_REG;
	}

	private int translateArithExpr(BinaryExpr expr, int alloc) {
		LValue fv = expr.foldedValue();
		if (fv != null) {
			return translateLValue(fv, alloc);
		}

		OpCode op = ExprUtil.toArithOpCode(expr.getOperator());
		assert op != null;

		int start = this.freeReg;
		int left, right;
		boolean needMerge = false;
		if (op == OpCode.CONCAT) {
			Expr re = expr.getRight();
			left = translateExpr(expr.getLeft(), RA_NEXT);
			right = translateExpr(re, RA_NEXT);
			needMerge = re instanceof BinaryExpr
				&& ((BinaryExpr)re).getOperator() == BinaryExpr.Operator.CONCAT;
		} else {
			left = translateExpr(expr.getLeft(), RA_RK);
			right = translateExpr(expr.getRight(), RA_RK);
		}
		this.freeReg = start;
		int result = requestReg(alloc);
		if (needMerge) {
			Instruction inst = ArrayUtil.get(getCode(), -1);
			assert inst.getOpCode() == OpCode.CONCAT && inst.getB() == left + 1;
			inst.setB(left);
			inst.setA(result);
		} else {
			instruction(new Instruction(op, result, left, right));
		}

		return result;
	}

	private int translateUnaryExpr(UnaryExpr expr, int alloc) {
		LValue fv = expr.foldedValue();
		if (fv != null) {
			return translateLValue(fv, alloc);
		}

		OpCode op = null;
		switch (expr.getOperator()) {
			case MINUS: op = OpCode.UNM; break;
			case NOT: op = OpCode.NOT; break;
			case LENGTH: op = OpCode.LEN; break;
		}

		int start = this.freeReg;
		int operand = translateExpr(expr.getOperand(), RA_ANY);
		this.freeReg = start;
		int result = requestReg(alloc);
		instruction(new Instruction(op, result, operand, 0));

		return result;
	}

	private int translatePrimaryExpr(PrimaryExpr expr, int alloc) {
		PrimaryExpr.Segment[] segments = expr.getSegments();
		Expr prefixExpr = expr.getPrefixExpr();

		int prefixAlloc;
		if (segments[0] instanceof PrimaryExpr.ArgsSegment) {
			prefixAlloc = RA_NEXT;
		} else {
			prefixAlloc = RA_ANY;
		}

		int prefix = translateExpr(prefixExpr, prefixAlloc);
		freeReg(prefix);
		final int base = this.freeReg;
		for (PrimaryExpr.Segment seg : expr.getSegments()) {
			if (seg instanceof PrimaryExpr.FieldSegment) {
				Expr key = ((PrimaryExpr.FieldSegment)seg).getKey();
				int rk = translateExpr(key, RA_RK);
				freeReg(rk);
				index(reserveReg(1), prefix, rk);
			} else if (seg instanceof PrimaryExpr.ArgsSegment) {
				Expr[] args = ((PrimaryExpr.ArgsSegment)seg).getArgs();
				translateArgs(args, reserveReg(1));
			} else if (seg instanceof PrimaryExpr.MethodSegment) {
				PrimaryExpr.MethodSegment fs = (PrimaryExpr.MethodSegment)seg;
				int rk = translateExpr(new LiteralString(fs.getName()), RA_RK);
				freeReg(rk);
				reserveReg(1);
				instruction(new Instruction(OpCode.SELF, base, prefix, rk));
				translateArgs(fs.getArgs(), base);
			}
			prefix = base;
			reserveReg(-1);
		}

		if (alloc >= 0) {
			if (expr.isIndexExpr()) {
				Instruction last = ArrayUtil.get(this.getCode(), -1);
				assert last.getOpCode() == OpCode.GETTABLE;
				last.setA(alloc);
			} else {
				move(alloc, base);
			}
			return alloc;
		} else {
			reserveReg(1);
			return base;
		}
	}

	private void translateArgs(Expr[] args, int func) {
		if (args != null) {
			for (Expr e : args) {
				translateExpr(e, RA_NEXT);
			}
		}
		int np = this.freeReg - func - 1;
		call(func, np, 1);
		reserveReg(-np);
	}

	private int translateVar(Var var, int alloc) {
		VarInfo info = singleVar(var.getName());
		int reg = Instruction.NO_REG;
		switch (info.type) {
			case VarInfo.LOCAL:
				if (alloc >= RA_NEXT) {
					reg = requestReg(alloc);
					move(reg, info.index);
				} else {
					reg = info.index;
				}
				break;
			case VarInfo.UPVALUE: {
				UpValue uv = this.upvalues.get(info.index);
				reg = requestReg(alloc);
				instruction(new Instruction(OpCode.GETUPVALUE, reg, uv.index, 0));
				break;
			}
			case VarInfo.GLOBAL: {
				int idx = addKString(var.getName());
				reg = requestReg(alloc);
				instruction(new Instruction(OpCode.GETGLOBAL, reg, idx));
				break;
			}
			default:
				assert false;
		}

		return reg;
	}

	private int translateFuncBody(FuncBody body, int alloc) {
		Translator t = new Translator(body.getChunk(), this);
		if (body.isNeedSelf()) {
			t.numParams++;
			t.addLocalVar("self");
		}
		FuncBody.Parlist params = body.getParlist();
		if (params != null) {
			t.vararg = params.isVararg();
			if (params.getParams() != null) {
				for (String p : params.getParams()) {
					t.numParams++;
					t.addLocalVar(p);
				}
			}
		}

		Proto p = t.translate();
		int reg = requestReg(alloc);
		this.ps.add(p);
		instruction(new Instruction(OpCode.CLOSURE, reg, this.ps.size() - 1));
		UpValue uv;
		for (int i = 0; i < t.upvalues.size(); ++i) {
			uv = t.upvalues.get(i);
			if (uv.inSameLevel) {
				instruction(new Instruction(OpCode.MOVE, 0, uv.index, 0));
			} else {
				instruction(new Instruction(OpCode.GETUPVALUE, 0, uv.index, 0));
			}
		}

		return reg;
	}

	private int translateKExpr(KExpr e, int alloc) {
		return translateLValue(e.toLuaValue(), alloc);
	}

	private int translateLValue(LValue v, int alloc) {
		int i = addK(v);
		if (alloc <= RA_RK) {
			return Instruction.setAsK(i);
		}

		int reg = requestReg(alloc);
		if (v instanceof LNil) {
			loadNil(reg);
		} else if (v instanceof LBoolean) {
			loadBoolean(((LBoolean) v).getValue(), reg);
		} else if (v instanceof LNumber || v instanceof LString) {
			loadK(i, reg);
		} else {
			assert false;
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

	private void patchToHere(List<Integer> js) {
		int pc = pc();
		patchList(js, pc, pc, Instruction.NO_REG);
	}

	private void patchList(List<Integer> list, int dest, int vdest, int reg) {
		if (ArrayUtil.isEmpty(list)) {
			return;
		}

		for (int jmp : list) {
			if (!patchTestReg(jmp, reg)) {
				fixJump(jmp, vdest);
			} else {
				fixJump(jmp, dest);
			}
		}
		list.clear();
	}

	private void fixJump(int jmp, int dest) {
		Instruction inst = getCode().get(jmp);
		inst.setBx(dest - jmp - 1);
	}

	private boolean patchTestReg(int jmp, int reg) {
		Instruction inst = ArrayUtil.get(getCode(), jmp - 1);
		if (inst.getOpCode() != OpCode.TESTSET) {
			return false;
		}

		if (reg == Instruction.NO_REG || reg == inst.getA()) {
			inst.setOpCode(OpCode.TEST);
			inst.setA(inst.getB());
			inst.setB(0);
		} else {
			inst.setA(reg);
		}
		return true;
	}

	private int condJump(OpCode op, int a, int b, int c) {
		instruction(new Instruction(op, a, b, c));
		instruction(new Instruction(OpCode.JMP, 0, Instruction.NO_JUMP));

		return pc() - 1;
	}

	private int compValue(int reg) {
		instruction(new Instruction(OpCode.LOADBOOL, reg, 0, 1));
		instruction(new Instruction(OpCode.LOADBOOL, reg, 1, 0));
		return pc() - 1;
	}

	private int addKNumber(String n) {
		return addK(new LNumber(Double.parseDouble(n)));
	}

	private int addKString(String s) {
		return addK(new LString(s));
	}

	private int addK(LValue v) {
		assert (v instanceof LNumber || v instanceof LString
			|| v instanceof LBoolean || v instanceof LNil);
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

		info.index = addKString(name);
		return info;
	}

	private void storeVar(AssignVarInfo info, int reg) {
		if (info.type == AssignVarInfo.LOCAL) {
			move(info.info, reg);
		} else if (info.type == AssignVarInfo.UPVALUE) {
			instruction(new Instruction(OpCode.SETUPVALUE, reg, info.info, 0));
		} else if (info.type == AssignVarInfo.GLOBAL) {
			instruction(new Instruction(OpCode.SETGLOBAL, reg, info.info));
		} else if (info.type == AssignVarInfo.TABLE) {
			int key = translateExpr(info.key, RA_RK);
			instruction(new Instruction(OpCode.SETTABLE, info.info, key, reg));
		} else {
			assert false;
		}
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
			assert nret >= -1;
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
		instruction(new Instruction(OpCode.LOADBOOL, reg, BoolUtil.toInt(v), 0));
	}

	private void loadK(int i, int reg) {
		instruction(new Instruction(OpCode.LOADK, reg, i));
	}

	private Instruction instruction(Instruction inst) {
		if (inst == null) {
			return null;
		}

		getCode().add(inst);
		return inst;
	}

	private ArrayList<Instruction> getCode() {
		return this.code;
	}

	private int pc() {
		return this.code.size();
	}

	private int requestReg(int alloc) {
		if (alloc >= 0) {
			return alloc;
		} else {
			return reserveReg(1);
		}
	}

	private int reserveReg(int n) {
		int reg = this.freeReg;
		this.freeReg += n;
		return reg;
	}

	private int freeReg(int reg) {
		if (reg == this.freeReg - 1 && reg >= this.actVars.size()) {
			return --this.freeReg;
		} else {
			return -1;
		}
	}

	private void checkRegAlloc(int alloc, int start, int result) {
		if (alloc >= 0) {
			assert (alloc == result && this.freeReg == start);
		} else if (alloc == RA_NEXT) {
			assert isNext(start, result);
		} else if (alloc == RA_ANY) {
			assert isAny(start, result);
		} else if (alloc == RA_RK) {
			assert isRK(start, result);
		} else if (alloc == RA_NONE) {
			assert isNone(start, result);
		} else {
			assert false;
		}
	}

	private boolean isNext(int start, int result) {
		return result == start && this.freeReg - result == 1;
	}

	private boolean isAny(int start, int result) {
		return (result < start && this.freeReg == start) || isNext(start, result);
	}

	private boolean isRK(int start, int result) {
		return Instruction.isK(result) || isAny(start, result);
	}

	private boolean isNone(int start, int result) {
		return result == Instruction.NO_REG || isRK(start, result);
	}

}
