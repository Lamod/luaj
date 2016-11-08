package lamo.luaj;

import lamo.luaj.parser.ParserException;
import lamo.luaj.parser.Parser;
import lamo.luaj.translator.Translator;
import lamo.luaj.base.Proto;

import java.io.*;

public class Luac {

	static public final int VERSION = 0x51;
	static public final int FORMAT = 0;
	static public final int HEADER_SIZE = 12;

	static public void main(String[] args) throws IOException, ParserException {
		String n = args[0];
		Parser p = new Parser(n, new FileReader(n));
		Proto func = new Translator(p.parse()).translate();
		p.close();

		File f = new File("luac.out");
		f.createNewFile();
		FileOutputStream out = new FileOutputStream(f);
		Dumper dumper = new Dumper(func, out, false);
		dumper.dump();
		out.close();
	}

}
