package lamo.luaj.parser.ast;

public class FuncExpr implements Expr {

	private FuncBody body;

	public FuncExpr(FuncBody body) {
		this.body = body;
	}

	public FuncBody getBody() {
		return this.body;
	}

	public void setBody(FuncBody body) {
		this.body = body;
	}

}
