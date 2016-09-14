package lamo.luaj.parser.ast;

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

}
