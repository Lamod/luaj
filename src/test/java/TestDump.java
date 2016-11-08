import lamo.luaj.Dumper;
import lamo.luaj.base.Proto;
import lamo.luaj.parser.Parser;
import lamo.luaj.parser.ParserException;
import lamo.luaj.parser.ast.Chunk;
import lamo.luaj.translator.Translator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;

public class TestDump {

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
				File f = new File("luac.out");
				f.createNewFile();
				FileOutputStream out = new FileOutputStream(f);
				Dumper dumper = new Dumper(fun, out, false);
				dumper.dump();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				p.close();
			}
		}
	}
}
