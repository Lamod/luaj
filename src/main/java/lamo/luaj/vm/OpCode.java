package lamo.luaj.vm;

public enum OpCode {

	MOVE(OpMode.iABC, false, true, ArgMode.R, ArgMode.N),
	LOADK(OpMode.iABx, false, true, ArgMode.K, ArgMode.N),
	LOADBOOL(OpMode.iABC, false, true, ArgMode.U, ArgMode.U),
	LOADNIL(OpMode.iABC, false, true, ArgMode.R, ArgMode.N),

	GETUPVALUE(OpMode.iABC, false, true, ArgMode.U, ArgMode.N),
	GETGLOBAL(OpMode.iABx, false, true, ArgMode.K, ArgMode.N),
	GETTABLE(OpMode.iABC, false, true, ArgMode.R, ArgMode.K),

	SETGLOBAL(OpMode.iABx, false, false, ArgMode.K, ArgMode.N),
	SETUPVALUE(OpMode.iABC, false, false, ArgMode.U, ArgMode.N),
	SETTABLE(OpMode.iABC, false, false, ArgMode.K, ArgMode.K),

	NEWTABLE(OpMode.iABC, false, true, ArgMode.U, ArgMode.U),

	SELF(OpMode.iABC, false, true, ArgMode.R, ArgMode.K),

	ADD(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),
	SUB(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),
	MUL(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),
	DIV(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),
	MOD(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),
	POW(OpMode.iABC, false, true, ArgMode.K, ArgMode.K),

	UNM(OpMode.iABC, false, true, ArgMode.R, ArgMode.N),
	NOT(OpMode.iABC, false, true, ArgMode.R, ArgMode.N),
	LEN(OpMode.iABC, false, true, ArgMode.R, ArgMode.N),
	CONCAT(OpMode.iABC, false, true, ArgMode.R, ArgMode.R),

	JMP(OpMode.iAsBx, false, false, ArgMode.R, ArgMode.N),

	EQ(OpMode.iABC, true, false, ArgMode.K, ArgMode.K),
	LT(OpMode.iABC, true, false, ArgMode.K, ArgMode.K),
	LE(OpMode.iABC, true, false, ArgMode.K, ArgMode.K),

	TEST(OpMode.iABC, true, true, ArgMode.R, ArgMode.U),
	TESTSET(OpMode.iABC, true, true, ArgMode.R, ArgMode.U),

	CALL(OpMode.iABC, false, true, ArgMode.U, ArgMode.U),
	TAILCALL(OpMode.iABC, false, true, ArgMode.U, ArgMode.U),

	RETURN(OpMode.iABC, false, false, ArgMode.U, ArgMode.N),

	FORLOOP(OpMode.iAsBx, false, true, ArgMode.R, ArgMode.N),
	FORPREP(OpMode.iAsBx, false, true, ArgMode.R, ArgMode.N),
	TFORLOOP(OpMode.iABC, true, false, ArgMode.N, ArgMode.U),

	SETLIST(OpMode.iABC, false, false, ArgMode.U, ArgMode.U),
	CLOSE(OpMode.iABC, false, false, ArgMode.N, ArgMode.N),
	CLOSURE(OpMode.iABx, false, true, ArgMode.U, ArgMode.N),
	VARARG(OpMode.iABC, false, true, ArgMode.U, ArgMode.N),
	;

	private OpMode opMode;
	private ArgMode bMode, cMode;
	private boolean aMode, tMode;

	OpCode(OpMode op, boolean t, boolean a, ArgMode b, ArgMode c) {
		this.opMode = op;
		this.bMode = b;
		this.cMode = c;
		this.aMode = a;
		this.tMode = t;
	}

	public OpMode getOpMode() {
		return opMode;
	}

	public boolean getTMode() {
		return tMode;
	}

	public boolean getAMode() {
		return aMode;
	}

	public ArgMode getBMode() {
		return bMode;
	}

	public ArgMode getCMode() {
		return cMode;
	}

	public int getIndex() {
		return ordinal();
	}

	public String getName() {
		return name();
	}

	public enum OpMode {
		iABC, iABx, iAsBx;
	}

	public enum ArgMode {
		N, U, R, K;
	}

	static public OpCode[] codes = values();
	
}
