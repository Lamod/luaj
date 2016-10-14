package lamo.luaj.vm;

public class Instruction {

	static public final int SIZE_OP = 6;
	static public final int SIZE_A = 8;
	static public final int SIZE_C = 9;
	static public final int SIZE_B = 9;
	static public final int SIZE_Bx = SIZE_B + SIZE_C;

	static public final int POS_OP = 0;
	static public final int POS_A = SIZE_OP;
	static public final int POS_C = POS_A + SIZE_A;
	static public final int POS_B = POS_C + SIZE_C;
	static public final int POS_Bx = POS_C;
	
	static public final int MAX_A = (1 << SIZE_A) - 1;
	static public final int MAX_B = (1 << SIZE_B) - 1;
	static public final int MAX_C = (1 << SIZE_C) - 1;
	static public final int MAX_Bx = (1 << SIZE_Bx) - 1;
	static public final int MAX_sBx = MAX_Bx >> 1; // sign bit

	static public final int NO_REG = MAX_A;
	static public final int NO_JUMP = -1;

	static public int mask1(int n, int p) {
		return ~(~(int)0 << n) << p;
	}

	static public int mask0(int n, int p) {
		return ~mask1(n, p);
	}

	static public final int BIT_RK = 1 << (SIZE_B - 1);
	static public final int MAX_INDEX_RK = BIT_RK - 1;

	static public boolean isK(int segment) {
		return (segment & BIT_RK) > 0;
	}

	static public boolean indexK(int segment) {
		return (segment & ~BIT_RK) > 0;
	}

	static public int asK(int v) {
		return v | BIT_RK;
	}

	private int value;

	// TODO: check format
	public Instruction(int value) {
		this.value = value;
	}

	public Instruction(OpCode code, int a, int b, int c) {
		assert code.getOpMode() == OpCode.OpMode.iABC;
		this.value = code.getIndex() | a << POS_A | b << POS_B | c << POS_C;
	}

	public Instruction(OpCode code, int a, int bx) {
		assert code.getOpMode() != OpCode.OpMode.iABC;
		this.value = code.getIndex() | a << POS_A | bx << POS_Bx;
	}

	public int getValue() {
		return value;
	}

	public OpCode getOpCode() {
		int i = getSegment(POS_OP, SIZE_OP);
		return OpCode.codes[i];
	}

	public int setOpCode(OpCode code) {
		int i = code.getIndex();
		return setSegment(i, POS_OP, SIZE_OP);
	}

	public int getA() {
		return getSegment(POS_A, SIZE_A);
	}

	public int setA(int a) {
		return setSegment(a, POS_A, SIZE_A);
	}

	public int setA(boolean a) {
		return setA(a ? 1 : 0);
	}

	public int getB() {
		return getSegment(POS_B, SIZE_B);
	}

	public int setB(int b) {
		return setSegment(b, POS_B, SIZE_B);
	}

	public int getC() {
		return getSegment(POS_C, SIZE_C);
	}

	public int setC(int c) {
		return setSegment(c, POS_C, SIZE_C);
	}

	public int setC(boolean c) {
		return setC(c ? 1 : 0);
	}

	public int getBx() {
		return getSegment(POS_Bx, SIZE_Bx);
	}

	public int setBx(int bx) {
		return setSegment(bx, POS_Bx, SIZE_Bx);
	}

	public int getsBx() {
		return getBx() - MAX_sBx;
	}
		
	public int setsBx(int sbx) {
		return setBx(sbx + MAX_sBx);
	}

	public String toString() {
		OpCode op = getOpCode();
		StringBuilder sb = new StringBuilder();
		sb.append(op.toString());
		sb.append(" ");
		sb.append(getA());
		sb.append(" ");

		switch (op.getOpMode()) {
			case iABC:
				if (formatArg(sb, getB(), op.getBMode())) {
					sb.append(" ");
				}
				formatArg(sb, getC(), op.getCMode());
				break;
			case iABx:
				sb.append(getBx());
				break;
			case iAsBx:
				sb.append(getsBx());
				break;
		}

		return sb.toString();
	}

	private boolean formatArg(StringBuilder sb, int v, OpCode.ArgMode mode) {
		switch (mode) {
			case K:
				if (isK(v)) {
					v = -((v & ~BIT_RK) + 1);
				}
			case U:case R:
				sb.append(v);
				return true;
			default:
				return false;
		}
	}

	private int getSegment(int pos, int size) {
		return (value >> pos) & mask1(size, 0);
	}

	private int setSegment(int v, int pos, int size) {
		assert(v <= (1 << size) - 1);

		value = (value & mask0(size, pos)) | ((v << pos) & mask1(size, pos));
		return value;
	}

}
