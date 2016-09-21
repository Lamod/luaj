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
		if (this.segments == null || this.segments.length == 0) {
			return false;
		}

		for (Segment seg : this.segments) {
			if (seg instanceof FuncArgsSegment) {
				return true;
			}
		}
		return false;
	}

	public boolean isFuncCallExpr() {
		return (ArrayUtil.get(this.segments, -1) instanceof FuncArgsSegment);
	}

	public boolean isVarExpr() {
		return ArrayUtil.isEmpty(this.segments);
	}

	public boolean isIndexExpr() {
		return (ArrayUtil.get(this.segments, -1) instanceof FieldSegment);
	}

	public boolean hasMultRet() {
		return isFuncCallExpr();
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.prefixExpr.toCode());
		if (this.segments != null) {
			Segment seg;
			for (int i = 0; i < this.segments.length; ++i) {
				seg = this.segments[i];
				if (seg instanceof FuncArgsSegment) {
					sb.append(seg.toCode());
				} else {
					Expr key = ((FieldSegment)seg).getKey();
					if (key instanceof LiteralString) {
						Segment next = ArrayUtil.get(this.segments, i + 1);
						if (next != null && next instanceof FuncArgsSegment
								&& ((FuncArgsSegment) next).isNeedSelf()) {
							sb.append(":");
						} else {
							sb.append(".");
						}
						sb.append(((LiteralString) key).getText());
					} else {
						sb.append(seg.toCode());
					}
				}
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
		private boolean needSelf;

		public Expr[] getArgs() {
			return this.args;
		}

		public void setArgs(Expr[] args) {
			this.args = args;
		}

		public boolean isNeedSelf() {
			return this.needSelf;
		}

		public void setNeedSelf(boolean needSelf) {
			this.needSelf = needSelf;
		}

		public String toCode() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(ArrayUtil.join(this.args, CODE_SERIALIZOR, ", "));
			sb.append(")");

			return sb.toString();
		}

	}

}
