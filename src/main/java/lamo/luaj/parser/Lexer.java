package lamo.luaj.parser;

import java.io.Reader;
import java.io.IOException;
import java.util.HashMap;

public class Lexer {

    private final static HashMap<String, Token> reversed = new HashMap<String, Token>();
    static {
        reversed.put("and", Token.AND);
        reversed.put("break", Token.BREAK);
        reversed.put("do", Token.DO);
        reversed.put("else", Token.ELSE);
        reversed.put("elseif", Token.ELSEIF);
        reversed.put("end", Token.END);
        reversed.put("false", Token.FALSE);
        reversed.put("for", Token.FOR);
        reversed.put("function", Token.FUNCTION);
        reversed.put("if", Token.IF);
        reversed.put("in", Token.IN);
        reversed.put("local", Token.LOCAL);
        reversed.put("return", Token.RETURN);
        reversed.put("nil", Token.NIL);
        reversed.put("not", Token.NOT);
        reversed.put("or", Token.OR);
        reversed.put("repeat", Token.REPEAT);
        reversed.put("then", Token.THEN);
        reversed.put("true", Token.TRUE);
        reversed.put("until", Token.UNTIL);
        reversed.put("while", Token.WHILE);
    };
    private final static char EOF_CHAR = (char)-1;

    private String fileName;
    private Reader reader;

    private int line = 1, column = 0;
    private char current = EOF_CHAR;
    private boolean closed = false;

    public Lexer(String fileName, Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        this.fileName = fileName;
        this.reader = reader;
        consume();
    }

    public Token next() throws LexerException {
        while (current != EOF_CHAR) {
            if (isWhiteSpace()) {
                consume();
                continue;
            }

            switch(current) {
                case '+':
                    consume();
                    return Token.ADD;
                case '-':
                    if (skipComment()) {
                        continue;
                    } else {
                        return Token.MINUS;
                    }
                case '*':
                    consume();
                    return Token.MULTI;
                case '/':
                    consume();
                    return Token.DIVIDE;
                case '%':
                    consume();
                    return Token.MODE;
                case '^':
                    consume();
                    return Token.POWER;
                case '#':
                    consume();
                    return Token.LENGTH;
                case '=':
                    consume();
                    if (current == '=') {
                        consume();
                        return Token.EQUAL;
                    } else {
                        return Token.ASSIGN;
                    }
                case '~':
                    consume();
                    match('=');
                    return Token.NOT_EQUAL;
                case '<':
                    consume();
                    if (current == '=') {
                        consume();
                        return Token.GREATE_EQUAL;
                    } else {
                        return Token.GREATE_THAN;
                    }
                case '>':
                    consume();
                    if (current == '=') {
                        consume();
                        return Token.LESS_EQUAL;
                    } else {
                        return Token.LESS_THAN;
                    }
                case '(':
                    consume();
                    return Token.LPARENT;
                case ')':
                    consume();
                    return Token.RPARENT;
                case '[': {
                    Token t = readLongString();
                    if (t == null) {
                        return Token.LBRACKET;
                    } else {
                        return t;
                    }
                }
                case ']':
                    consume();
                    return Token.RBRACKET;
                case '{':
                    consume();
                    return Token.LBRACE;
                case '}':
                    consume();
                    return Token.RBRACE;
                case '\'':
                case '"':
                    return readString();
                case ',':
                    consume();
                    return Token.COMMA;
                case ':':
                    consume();
                    return Token.COLON;
                case ';':
                    consume();
                    return Token.SEMICOLON;
                case '.': {
                    Token t = readNumber();
                    if (t != null) {
                        return t;
                    }
                    consume();
                    if (current != '.') {
                        return Token.DOT;
                    }
                    consume();
                    if (current != '.') {
                        return Token.CONCAT;
                    }
                    consume();
                    return Token.DOTS;
                }
                default:
                    if (isDigit()) {
                        return readNumber();
                    } else if (isAlpha() || current == '_') {
                        return readName();
                    }
            }

            throw new LexerException(line, column, "unexpected character " + current);
        }

        if (!closed) {
            close();
        }
        return Token.EOF;
    }

    public void close() {
        if (closed) {
            return;
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        if (current == '0') {
            sb.append(consume());
            if (current == 'x' || current == 'X') {
                sb.append(consume());
                sb.append(readHex());

                return Token.NUMBER.withText(sb.toString());
            }
        } else if (current == '.') {
            sb.append(consume());
            if (!isDigit()) {
                return null;
            }
        }

        while (isDigit() || current == '.') {
            sb.append(consume());
        }
        if (current == 'E' || current == 'e') {
            sb.append(consume());
            if (current == '+' || current == '-') {
                sb.append(consume());
            }
            while (isDigit()) {
                sb.append(consume());
            }
        }

        return Token.NUMBER.withText(sb.toString());
    }

    private String readHex() {
        StringBuilder sb = new StringBuilder();
        while (isHex()) {
            sb.append(consume());
        }

        return sb.toString();
    }

    private Token readName() {
        StringBuilder sb = new StringBuilder();

        do {
            sb.append(consume());
        } while (isDigit() || isAlpha() || current == '_');

        String name = sb.toString();
        Token t = reversed.get(name);
        if (t != null) {
            return t;
        } else {
            return Token.NAME.withText(name);
        }
    }

    private Token readString() throws LexerException {
        char starter = current;
        int startLine = line;
        int startColumn = column;
        consume();
        StringBuilder sb = new StringBuilder();

        while (current != starter) {
            if (current == EOF_CHAR) {
                throw new LexerException(startLine, startColumn, "unfinished string");
            } else if (current == '\\') {
                consume();
                sb.append(readEscape());
            } else {
                sb.append(consume());
            }
        }
        consume();

        return Token.STRING.withText(sb.toString());
    }

    private char readEscape() throws LexerException {
        switch (current) {
            case '"':
                consume();
                return '"';
            case '\'':
                consume();
                return '\'';
            case '?':
                consume();
                return '?';
            case '\\':
                consume();
                return '\\';
            case '/':
                consume();
                return '/';
            case 'b':
                consume();
                return '\b';
            case 'f':
                consume();
                return '\f';
            case 't':
                consume();
                return '\t';
            case 'n':
                consume();
                return '\n';
            case 'r':
                consume();
                return '\r';
            case '\n':
            case '\r':
                consume();
                return '\n';
            default:
                if (isDigit()) {
                    return readNumberalEscape();
                }
        }

        throw new LexerException(line, column, "unsupported escape sequence " + current);
    }

    private char readNumberalEscape() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (isDigit() && i < 3) {
            sb.append(consume());
            i++;
        }

        try {
            return (char)Integer.parseInt(sb.toString());
        } catch (Exception e) {
            return (char)0;
        }
    }

    private Token readLongString() throws LexerException {
        int startLine = line;
        int startColumn = column;
        int level = readLongBracketLevel();
        if (level < -1) {
            throw new LexerException(startLine, startColumn, "invalid long string delimiter");
        } else if (level == -1) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        while (current != EOF_CHAR) {
            if (first && isNewLine()) {
                first = false;
                consume();
                continue;
            }

            if (current != ']') {
                sb.append(consume());
                continue;
            }
            int closeLevel = readLongBracketLevel();
            if (closeLevel != level) {
                sb.append(regainClosingLongBracket(closeLevel));
                continue;
            }

            return Token.STRING.withText(sb.toString());
        }

        throw new LexerException(startLine, startColumn, "unfinished long string");
    }

    private int readLongBracketLevel() {
        char starter = current;
        int level = 0;
        consume();

        while (current == '=') {
            level++;
            consume();
        }

        if (current == starter) {
            consume();
            return level;
        } else {
            return -1 - level;
        }
    }

    private String regainClosingLongBracket(int level) {
        StringBuilder sb = new StringBuilder();
        sb.append(']');

        int count = level > 0 ? level : (-level - 1);
        for (int i = 0; i < count; i++) {
            sb.append('=');
        }
        if (level >= 0) {
            sb.append(']');
        }

        return sb.toString();
    }

    private boolean skipComment() throws LexerException {
        int startLine = line;
        int startColumn = column;
        consume();
        if (current != '-') {
            return false;
        }

        consume();
        // single line comment
        if (current != '[') {
            while (!isNewLine()) {
                consume();
            }
            consume();

            return true;
        }

        // long comment
        int level = readLongBracketLevel();
        while (current != EOF_CHAR) {
            if (current != ']') {
                consume();
                continue;
            }

            int closeLevel = readLongBracketLevel();
            if (level != closeLevel) {
                continue;
            }

            // end of long comment
            return true;
        }

        throw new LexerException(startLine, startColumn, "unfinished long comment");
    }

    private char consume() {
        if (isNewLine()) {
            line++;
            column = 1;
        } else {
            column++;
        }

        char c = current;
        if (closed) {
            current = EOF_CHAR;
        } else {
            try {
                current = (char)reader.read();
            } catch(Exception e) {
                current = EOF_CHAR;
            }
        }

        return c;
    }

    private boolean tryMatch(char x) {
        return current == x;
    }

    private char match(char x) throws LexerException {
        if (tryMatch(x)) {
            return x;
        } else {
            throw new LexerException(line, column, "expecting " + x + ", got " + current);
        }
    }

    private boolean isNewLine() {
        return current == '\n' || current == '\r';
    }

    private boolean isWhiteSpace() {
        return Character.isWhitespace(current);
    }

    private boolean isAlpha() {
        return Character.isLetter(current);
    }

    private boolean isDigit() {
        return Character.isDigit(current);
    }

    private boolean isHex() {
        return isDigit() || current >= 'a' && current <= 'f' || current >= 'A' && current <= 'F';
    }

}
