package lamo.luaj.parser.ast;

import lamo.luaj.parser.Token;
import lamo.luaj.parser.Token.TType;

public class BinaryExpr implements Expr {

	public enum Operator {
 		OR(0, "or"),
		AND(1, "and"),
		GREATE_THAN(2, ">"), GREATE_EQUAL(2, ">="), LESS_THAN(2, "<"), LESS_EQUAL(2, "<="), EQUAL(2, "=="), NOT_EQUAL(2, "~="),
		CONCAT(3, true, ".."),
		ADD(4, "+"), MINUS(4, "-"),
		MULTI(5, "*"), DIVIDE(5, "/"), MODE(5, "%"),
		POWER(7, true, "^"), 
		;

		private int precedence;
		private boolean rightAssociative;
		private String token;

		private Operator(int precedence, String token) {
			this.precedence = precedence;
			this.rightAssociative = false;
			this.token = token;
		}

		private Operator(int precedence, boolean rightAssociative, String token) {
			this.precedence = precedence;
			this.rightAssociative = rightAssociative;
			this.token = token;
		}

		public int getPrecedence() {
			return this.precedence;
		}

		public boolean isLeftAssocitive() {
			return !this.rightAssociative;
		}

		public boolean isRightAssocitive() {
			return this.rightAssociative;
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

	static public BinaryExpr adjust(Expr left, Operator op, Expr right) {
		if (left instanceof BinaryExpr && !((BinaryExpr)left).getClosed()) {
			return ((BinaryExpr)left).adjust(op, right);
		} else {
			return new BinaryExpr(left, op, right);
		}
	}

	private Expr left, right;
	private Operator operator;
	private boolean closed = false;

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

	private BinaryExpr adjust(Operator op, Expr expr) {
		if (op.precedence < this.operator.precedence) {
			return adjustToLeft(op, expr);
		} else if (op.precedence > this.operator.precedence) {
			return adjustToRight(op, expr);
		} else if (op.isRightAssocitive()) {
			return adjustToRight(op, expr);
		} else {
			return adjustToLeft(op, expr);
		}
	}

	private BinaryExpr adjustToRight(Operator op, Expr expr) {
		return new BinaryExpr(this, op, expr);
	}

	private BinaryExpr adjustToLeft(Operator op, Expr expr) {
		BinaryExpr newExpr = new BinaryExpr(this.left, op, expr);
		this.left = newExpr;

		return this;	
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.closed) {
			sb.append("(");
		}
		sb.append(this.left.toString());
		sb.append(" ");
		sb.append(this.operator.toString());
		sb.append(" ");
		sb.append(this.right.toString());
		if (this.closed) {
			sb.append(")");
		}

		return sb.toString();
	}

}
