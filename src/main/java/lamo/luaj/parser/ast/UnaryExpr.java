package lamo.luaj.parser.ast;

import lamo.luaj.parser.Token;
import lamo.luaj.parser.Token.TType;

public class UnaryExpr implements Expr {

	static public final int OP_PRIORITY = 8;

	public enum Operator {
		NEGATIVE("-"), LENGTH("#"), NOT("not");

		private String token;

		private Operator(String token) {
			this.token = token;
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

	static public Operator getOperator(TType tokenType) {
		switch (tokenType) {
			case MINUS: return Operator.NEGATIVE;
			case LENGTH: return Operator.LENGTH;
			case NOT: return Operator.NOT;
			default: return null;
		}
	}

	private Expr operand;
	private Operator operator;

	public Expr getOperand() {
		return this.operand;
	}

	public void setOperand(Expr operand) {
		this.operand = operand;
	}

	public Operator getOperator() {
		return this.operator;
	}

	public void setOperator(Operator op) {
		this.operator = op;
	}

	public String toString() {
		return this.operator.toString() + this.operand.toString();
	}

}
