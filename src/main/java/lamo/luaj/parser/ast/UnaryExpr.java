package lamo.luaj.parser.ast;

import lamo.luaj.base.LBoolean;
import lamo.luaj.base.LNumber;
import lamo.luaj.base.LValue;
import lamo.luaj.parser.Token;
import lamo.luaj.parser.Token.TType;

public class UnaryExpr extends Expr implements Foldable {

	static public final int OP_PRIORITY = 8;

	public enum Operator {
		MINUS("-"), LENGTH("#"), NOT("not");

		private String token;

		Operator(String token) {
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
			case MINUS: return Operator.MINUS;
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

	public LValue foldedValue() {
		switch (this.operator) {
			case MINUS:
				if (this.operand instanceof LiteralNumber) {
					return new LNumber(-Double.parseDouble(((LiteralNumber) this.operand).getText()));
				} else if (this.operand instanceof BinaryExpr) {
					LNumber n = ((BinaryExpr) this.operand).foldedValue();
					return new LNumber(-n.getValue());
				}
				break;
			case NOT:
				if (operand instanceof Nil || operand instanceof False) {
					return LBoolean.TRUE;
				} else if (operand instanceof True
						|| operand instanceof LiteralNumber
						|| operand instanceof LiteralString) {
					return LBoolean.FALSE;
				}
				break;
			default:
				break;
		}
		return null;
	}

	public String toCode() {
		return this.operator.toString() + " " + this.operand.toCode();
	}

}
