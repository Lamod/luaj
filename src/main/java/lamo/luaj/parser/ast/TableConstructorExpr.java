package lamo.luaj.parser.ast;

public class TableConstructorExpr implements Expr {

	private Field[] fields;

	public Field[] getFields() {
		return this.fields;
	}

	public void setFields(Field[] fields) {
		this.fields = fields;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if (this.fields != null) {
			boolean first = true;
			for (Field f : this.fields) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(f.toString());
			}
		}
		sb.append("}");

		return sb.toString();
	}

	static abstract public class Field {

		private Expr value;

		public Field(Expr value) {
			this.value = value;
		}

		public Expr getValue() {
			return this.value;
		}

		public void setValue(Expr value) {
			this.value = value;
		}

	}

	static public class ListField extends Field {

		private int index;
		
		public ListField(Expr value) {
			super(value);
		}

		public int getIndex() {
			return this.index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String toString() {
			return this.getValue().toString();
		}

	}

	static public class NameField extends Field {

		private String key;

		public NameField(String key, Expr value) {
			super(value);
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String toString() {
			return this.key + " = " + this.getValue().toString();
		}

	}

	static public class ExprField extends Field {

		private Expr key;

		public ExprField(Expr key, Expr value) {
			super(value);
			this.key = key;
		}

		public Expr getKey() {
			return this.key;
		}

		public void setKey(Expr key) {
			this.key = key;
		}

		public String toString() {
			return "[" + this.key.toString() + "]" + " = " + this.getValue().toString();
		}

	}

}
