package lamo.luaj.parser.ast;

public class Var extends Expr {

	private String name;

	public Var(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toCode() {
		return this.name;
	}

}
