import lamo.luaj.vm.*;

public class TestOpCode {

	static public void main(String[] args) {
		OpCode[] codes = OpCode.codes;
		for (OpCode c : codes) {
			System.out.println(c);
		}
	}

}
