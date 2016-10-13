package lamo.luaj.translator;

import lamo.luaj.parser.ast.BinaryExpr;
import lamo.luaj.parser.ast.Expr;
import lamo.luaj.parser.ast.PrimaryExpr;
import lamo.luaj.util.ArrayUtil;
import lamo.luaj.vm.OpCode;

class ExprUtil {

	static boolean isArithExpr(Expr expr) {
		return expr instanceof BinaryExpr && isArithExpr((BinaryExpr)expr);
	}

	static boolean isArithExpr(BinaryExpr expr) {
		return toArithOpCode(expr.getOperator()) != null;
	}

	static boolean isCompExpr(Expr expr) {
		return expr instanceof BinaryExpr && isCompExpr((BinaryExpr)expr);
	}

	static boolean isCompExpr(BinaryExpr expr) {
		return toCompOpCode(expr.getOperator()) != null;
	}

	static boolean isLogicalExpr(Expr expr) {
		return expr instanceof BinaryExpr && isLogicalExpr((BinaryExpr)expr);
	}

	static boolean isLogicalExpr(BinaryExpr expr) {
		BinaryExpr.Operator op = expr.getOperator();
		return op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR;
	}

	static OpCode toArithOpCode(BinaryExpr.Operator op) {
		switch (op) {
			case ADD: return OpCode.ADD;
			case SUB: return OpCode.SUB;
			case MULTI: return OpCode.MUL;
			case DIVIDE: return OpCode.DIV;
			case MODE: return OpCode.MOD;
			case POWER: return OpCode.POW;
			case CONCAT: return OpCode.CONCAT;
			default: return null;
		}
	}

	static OpCode toCompOpCode(BinaryExpr.Operator op) {
		switch (op) {
			case NOT_EQUAL: case EQUAL:
				return OpCode.EQ;
			case GREATE_THAN: case LESS_THAN:
				return OpCode.LT;
			case GREATE_EQUAL: case LESS_EQUAL:
				return OpCode.LE;
			default:
				return null;
		}
	}

	static Expr reduce(Expr expr) {
		if (expr instanceof PrimaryExpr) {
			PrimaryExpr pe = (PrimaryExpr)expr;
			if (ArrayUtil.isEmpty(pe.getSegments())) {
				return pe.getPrefixExpr();
			}
		}

		return expr;
	}

}
