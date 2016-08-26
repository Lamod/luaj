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

	public boolean isFuncCallExpr() {
		return segments != null
			&& segments.length > 0
			&& !(segments[segments.length - 1] instanceof FieldSegment);
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.prefixExpr.toCode());
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

	static public class FuncArgsSegment extends Segment {

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
			sb.append(ArrayUtil.join(this.args, CODE_SERIALIZOR, ", "));
			sb.append(")");

			return sb.toString();
		}

	}

	static public class FieldAndArgsSegment extends Segment {

		private String key;
		private Expr[] args;

		public String getKey() {
			return this.key;
		}

		public void setKey(String key) {
			this.key = key;
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
			sb.append(this.key);
			sb.append("(");
			sb.append(ArrayUtil.join(this.args, CODE_SERIALIZOR, ", "));
			sb.append(")");

			return sb.toString();
		}

	}

}
