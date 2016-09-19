package lamo.luaj.parser.ast;

import lamo.luaj.LString;
import lamo.luaj.LValue;

public class LiteralString extends KExpr {

	private String text;

	public LiteralString(String text) {
		this.text = text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public String toCode() {
		return "\"" + text + "\"";
	}

	public LValue toLuaValue() {
		return new LString(this.text);
	}

}
