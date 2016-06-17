import lamo.luaj.parser.*;
import java.io.FileReader;

public class TestLexer {

    public static void main(String[] args) throws Exception {
        for (String file: args) {
            FileReader reader = new FileReader(file);
            System.out.println("=== " + file + "(" + reader.getEncoding() + ") ===");

            Lexer l = new Lexer(file, new FileReader(file));
            try {
                Token t = l.next();
                while (t != Token.EOF) {
                    System.out.println(t);
                    t = l.next();
                }
            }
            finally {
                l.close();
            }
        }
    }

}
