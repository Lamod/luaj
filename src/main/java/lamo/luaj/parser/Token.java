package lamo.luaj.parser;

public enum Token {
    EOF, // EOF
    ASSIGN, // =
    LENGTH, // #
    ADD, // +
    MINUS, // -
    MULTI, // *
    DIVIDE, // /
    POWER, // ^
    MODE, // %
    GREATE_THAN, // >
    GREATE_EQUAL, // >=
    LESS_THAN, // <
    LESS_EQUAL, // <=
    EQUAL, // ==
    NOT_EQUAL, // ~=
    NOT, // not
    AND, // and
    OR, // or
    DOT, // .
    CONCAT, // ..
    DOTS, // ...
    COMMA, // ,
    COLON, // :
    SEMICOLON, // ;
    LBRACE, // {
    RBRACE, // }
    LBRACKET, // [
    RBRACKET, // ]
    LPARENT, // {
    RPARENT, // }
    BREAK, // break
    DO, // do
    END, // end
    WHILE, // while
    REPEAT, // repeat
    UNTIL, // until
    IF, // if
    THEN, // then
    ELSE, // else
    ELSEIF, // elseif
    FOR, // for
    IN, // in
    LOCAL, // local
    FUNCTION, // function
    RETURN, // return
    NIL, // nil
    TRUE, // true
    FALSE, // false
    NAME,
    NUMBER,
    STRING,
    ;

    private String text;
    
    public Token withText(String text) {
        setText(text);
        return this;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String toString() {
        String s = "<Token type:" + super.toString();
        if (text != null) {
            s = s + " text:\"" + text + "\"";
        }

        s = s + ">";

        return s;
    }

}
