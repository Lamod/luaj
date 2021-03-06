import lamo.luaj.parser.*;
import lamo.luaj.parser.Token.TType;

import java.io.FileReader;

public class TestLexer {

    public static void main(String[] args) throws Exception {
        for (String file: args) {
            FileReader reader = new FileReader(file);
            System.out.println("=== " + file + "(" + reader.getEncoding() + ") ===");

            Lexer l = new Lexer(file, new FileReader(file));
            Token t = l.next();
            while (t.getType() != TType.EOF) {
                System.out.println(t);
                t = l.next();
            }
            l.close();
        }
    }

}
