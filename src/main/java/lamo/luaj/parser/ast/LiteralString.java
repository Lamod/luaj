package lamo.luaj.parser.ast;

public class LiteralString extends Expr {

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

}
