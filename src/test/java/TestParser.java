import lamo.luaj.parser.*;
import lamo.luaj.parser.ast.*;
import java.io.FileReader;

public class TestParser {

    public static void main(String[] args) throws Exception {
        for (String file: args) {
            FileReader reader = new FileReader(file);
            System.out.println("=== " + file + "(" + reader.getEncoding() + ") ===");

			Parser p = new Parser(file, new FileReader(file));

			try {
				Chunk c = p.parse();
				System.out.println(c);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				p.close();
			}
        }
    }

}
