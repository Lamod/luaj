package lamo.luaj.parser.ast;

public class FuncExpr extends Expr {

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

	public String toCode() {
		StringBuilder sb = new StringBuilder();
		sb.append("function");
		sb.append(this.body.toCode());
		sb.append(this.body.getChunk().getIntend().substring(INDENT_SIZE));
		sb.append("end");

		return sb.toString();
	}

}
