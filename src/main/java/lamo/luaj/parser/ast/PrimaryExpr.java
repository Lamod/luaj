package lamo.luaj.parser.ast;

public class PrimaryExpr implements Expr {

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

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.prefixExpr);
		if (this.segments != null) {
			for (Segment seg : this.segments) {
				sb.append(seg);
			}
		}

		return sb.toString();
	}

	static public abstract class Segment {
	}

	static public class FieldSegment extends Segment {

		private Expr key;

		public Expr getKey() {
			return this.key;
		}

		public void setKey(Expr key) {
			this.key = key;
		}

		public String toString() {
			return "[" + key + "]";
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

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			boolean first = true;
			for (Expr arg : this.args) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(arg);
			}
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

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(":");
			sb.append(this.key);
			sb.append("(");
			boolean first = true;
			for (Expr arg : this.args) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(arg);
			}
			sb.append(")");

			return sb.toString();
		}

	}

}
