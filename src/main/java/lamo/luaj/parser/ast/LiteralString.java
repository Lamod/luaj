package lamo.luaj.parser.ast;

public class LiteralString implements Expr {

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

	public String toString() {
		return "\"" + text + "\"";
	}

}
