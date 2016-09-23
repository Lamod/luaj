import lamo.luaj.parser.Parser;
import lamo.luaj.parser.ParserException;
import lamo.luaj.parser.ast.Chunk;
import lamo.luaj.translator.Translator;
import lamo.luaj.vm.Proto;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class TestTranslator {

	public static void main(String[] args) throws FileNotFoundException, ParserException {
		for (String file: args) {
			if (file.startsWith("-")) {
				continue;
			}
			FileReader reader = new FileReader(file);
			System.out.println("=== " + file + "(" + reader.getEncoding() + ") ===");

			Parser p = new Parser(file, new FileReader(file));

			try {
				Chunk c = p.parse();
				Translator t = new Translator(c);
				Proto fun = t.translate();
				System.out.println(fun);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				p.close();
			}
		}
	}

}
