package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class PrimaryExpr extends Expr {

	private Expr prefixExpr;
	private Segment[] segments;

	public Expr getPrefixExpr() {
		return this.prefixExpr;
	}

	public void setPrefixExpr(Expr prefixExpr) {
		this.prefixExpr = prefixExpr;
	}

	public Segment[] getSegments() {
		return this.segments;
	}

	public void setSegments(Segment[] segments) {
		this.segments = segments;
	}

	public boolean hasFuncCallSeg() {
		if (ArrayUtil.isEmpty(this.segments)) {
			return false;
		}

		for (Segment seg : this.segments) {
			if (!(seg instanceof FieldSegment)) {
				return true;
			}
		}
		return false;
	}

	public boolean isFuncCallExpr() {
		return !ArrayUtil.isEmpty(this.segments)
			&& !(ArrayUtil.get(this.segments, -1) instanceof FieldSegment);
	}

	public boolean isIndexExpr() {
		return !ArrayUtil.isEmpty(this.segments)
			&& (ArrayUtil.get(this.segments, -1) instanceof FieldSegment);
	}

	public boolean isAssignable() {
		if (!ArrayUtil.isEmpty(this.segments)) {
			return isIndexExpr();
		}
		if (this.prefixExpr instanceof Var) {
			return true;
		} else if (this.prefixExpr instanceof PrimaryExpr) {
			return ((PrimaryExpr)this.prefixExpr).isAssignable();
		} else {
			return false;
		}
	}

	public boolean hasMultRet() {
		return isFuncCallExpr();
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		if (this.prefixExpr instanceof Var) {
			sb.append(this.prefixExpr.toCode());
		} else {
			sb.append("(");
			sb.append(this.prefixExpr.toCode());
			sb.append(")");
		}
		if (this.segments != null) {
			for (Segment seg : this.segments) {
				sb.append(seg.toCode());
			}
		}

		return sb.toString();
	}

	static public abstract class Segment extends Node {
	}

	static public class FieldSegment extends Segment {

		private Expr key;

		public Expr getKey() {
			return this.key;
		}

		public void setKey(Expr key) {
			this.key = key;
		}

		public String toCode() {
			return "[" + this.key.toCode() + "]";
		}

	}

	static public class ArgsSegment extends Segment {

		private Expr[] args;

		public Expr[] getArgs() {
			return this.args;
		}

		public void setArgs(Expr[] args) {
			this.args = args;
		}

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(ArrayUtil.join(this.args, CODE_SERIALIZER, ", "));
			sb.append(")");

			return sb.toString();
		}

	}

	static public class MethodSegment extends Segment {

		private String name;
		private Expr[] args;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Expr[] getArgs() {
			return this.args;
		}

		public void setArgs(Expr[] args) {
			this.args = args;
		}

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			sb.append(":");
			sb.append(this.name);
			sb.append("(");
			sb.append(ArrayUtil.join(this.args, CODE_SERIALIZER, ", "));
			sb.append(")");

			return sb.toString();
		}

	}

}
