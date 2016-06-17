package lamo.luaj.parser;

public class LexerException extends Exception {

    private int line, column;

    public LexerException(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public LexerException(int line, int column, String message) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public LexerException(int line, int column, String message, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
    }

    public String toString() {
        return super.toString() + " at line(" + line + ") column(" + column + ")";
    }

}
