package lamo.luaj.parser.ast;

import lamo.luaj.util.ArrayUtil;

public class TableConstructorExpr extends Expr {

	private Field[] fields;

	public Field[] getFields() {
		return this.fields;
	}

	public void setFields(Field[] fields) {
		this.fields = fields;
	}

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(ArrayUtil.join(this.fields, CODE_SERIALIZER, ", "));
		sb.append("}");

		return sb.toString();
	}

	static abstract public class Field extends Node {

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

		public String toCode() {
			return this.getValue().toCode();
		}

	}

	static public class RecField extends Field {

		private Expr key;

		public RecField(Expr key, Expr value) {
			super(value);
			this.key = key;
		}

		public Expr getKey() {
			return this.key;
		}

		public void setKey(Expr key) {
			this.key = key;
		}

		public String toCode() {
			return "[" + this.key.toCode() + "]" + " = " + this.getValue().toCode();
		}

	}

}
