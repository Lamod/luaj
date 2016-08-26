package lamo.luaj.parser.ast;

public class BreakStat extends LastStat {

	public String toCode() {
		return getIntend() + "break\n";
	}

}
