package lamo.luaj.parser.ast;

import lamo.luaj.parser.Token;
import lamo.luaj.parser.Token.TType;

public class BinaryExpr extends Expr {

	public enum Operator {
 		OR(1, 1, "or"),
		AND(2, 2, "and"),
		GREATE_THAN(3, 3, ">"), GREATE_EQUAL(3, 3, ">="), LESS_THAN(3, 3, "<"), LESS_EQUAL(3, 3, "<="),
		EQUAL(3, 3, "=="), NOT_EQUAL(3, 3, "~="),
		CONCAT(5, 4, ".."),
		ADD(6, 6, "+"), MINUS(6, 6, "-"),
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
			case GREATE_THAN: return Operator.GREATE_THAN;
			case GREATE_EQUAL: return Operator.GREATE_EQUAL;
			case LESS_THAN: return Operator.LESS_THAN;
			case LESS_EQUAL: return Operator.LESS_EQUAL;
			case EQUAL: return Operator.EQUAL;
			case NOT_EQUAL: return Operator.NOT_EQUAL;
			case ADD: return Operator.ADD;
			case MINUS: return Operator.MINUS;
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

}
