package lamo.luaj.parser.ast;

import lamo.luaj.base.LNumber;
import lamo.luaj.base.LValue;
import lamo.luaj.parser.Token;
import lamo.luaj.parser.Token.TType;

public class BinaryExpr extends Expr implements Foldable {

	public enum Operator {
 		OR(1, 1, "or"),
		AND(2, 2, "and"),
		GREATE_THAN(3, 3, ">"), GREATE_EQUAL(3, 3, ">="), LESS_THAN(3, 3, "<"), LESS_EQUAL(3, 3, "<="),
		EQUAL(3, 3, "=="), NOT_EQUAL(3, 3, "~="),
		CONCAT(5, 4, ".."),
		ADD(6, 6, "+"), SUB(6, 6, "-"),
		MULTI(7, 7, "*"), DIVIDE(7, 7, "/"), MODE(7, 7, "%"),
		POWER(10, 9, "^"), 
		;

		private final int leftPriority, rightPriority;
		private final String token;

		Operator(int left, int right, String token) {
			this.leftPriority = left;
			this.rightPriority = right;
			this.token = token;
		}

		public int getLeftPriority() {
			return this.leftPriority;
		}

		public int getRightPriority() {
			return this.rightPriority;
		}

		public String getToken() {
			return this.token;
		}

		public String toString() {
			return this.token;
		}

	}

	static public Operator getOperator(Token token) {
		return getOperator(token.getType());
	}

	static public Operator getOperator(TType type) {
		switch (type) {
			case OR: return Operator.OR;
			case AND: return Operator.AND;
			case GREAT_THAN: return Operator.GREATE_THAN;
			case GREAT_EQUAL: return Operator.GREATE_EQUAL;
			case LESS_THAN: return Operator.LESS_THAN;
			case LESS_EQUAL: return Operator.LESS_EQUAL;
			case EQUAL: return Operator.EQUAL;
			case NOT_EQUAL: return Operator.NOT_EQUAL;
			case CONCAT: return Operator.CONCAT;
			case ADD: return Operator.ADD;
			case MINUS: return Operator.SUB;
			case MULTI: return Operator.MULTI;
			case DIVIDE: return Operator.DIVIDE;
			case MODE: return Operator.MODE;
			case POWER: return Operator.POWER;
			default: return null;
		}
	}

	private Expr left, right;
	private Operator operator;
	private boolean closed;

	public BinaryExpr(Expr left, Operator op, Expr right) {
		this.left = left;
		this.right = right;
		this.operator = op;
	}

	public BinaryExpr() {
		this.left = null;
		this.right = null;
		this.operator = null;
	}

	public Expr getLeft() {
		return this.left;
	}

	public void setLeft(Expr left) {
		this.left = left;
	}

	public Expr getRight() {
		return this.right;
	}

	public void setRight(Expr right) {
		this.right = right;
	}

	public Operator getOperator() {
		return this.operator;
	}

	public void setOperator(Operator op) {
		this.operator = op;
	}

	public boolean getClosed() {
		return this.closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public LNumber foldedValue() {
		LNumber ln = operandToLuaNumber(this.left), rn = operandToLuaNumber(this.right);
		if (ln == null || rn == null) {
			return null;
		}

		double l = ln.getValue(), r = rn.getValue(), res = 0;
		switch (this.operator) {
			case ADD:
				res = l + r;
				break;
			case SUB:
				res = l - r;
				break;
			case MULTI:
				res = l * r;
				break;
			case DIVIDE:
				res = l / r;
				break;
			case MODE:
				if (r == 0) {
					return null;
				} else {
					res = l % r;
				}
				break;
			case POWER:
				res = Math.pow(l, r);
				break;
			default:
				return null;

		}
		return new LNumber(res);
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		if (this.closed) {
			sb.append("(");
		}
		sb.append(this.left.toCode());
		sb.append(" ");
		sb.append(this.operator.toString());
		sb.append(" ");
		sb.append(this.right.toCode());
		if (this.closed) {
			sb.append(")");
		}

		return sb.toString();
	}

	private static LNumber operandToLuaNumber(Expr operand) {
		if (operand instanceof LiteralNumber) {
			return new LNumber(Double.parseDouble(((LiteralNumber) operand).getText()));
		} else if (operand instanceof Foldable) {
			LValue v = ((Foldable) operand).foldedValue();
			return (v instanceof LNumber) ? (LNumber)v : null;
		} else {
			return null;
		}
	}

}
