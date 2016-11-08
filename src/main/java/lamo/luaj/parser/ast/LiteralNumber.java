package lamo.luaj.parser.ast;

import lamo.luaj.base.LNumber;
import lamo.luaj.base.LValue;

public class LiteralNumber extends KExpr {

	private String text;

	public LiteralNumber(String text) {
		this.text = text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public String toCode() {
		return text;
	}

	public LValue toLuaValue() {
		return new LNumber(Double.parseDouble(this.text));
	}

}
