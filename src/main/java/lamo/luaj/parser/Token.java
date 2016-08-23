package lamo.luaj.parser;

public class Token {

	public enum TType  {
		EOF, // EOF
		ASSIGN, // =
		LENGTH, // #
		ADD, // +
		MINUS, // -
		MULTI, // *
		DIVIDE,// /
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
		LPARENT, // (
		RPARENT, // )
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
		STRING;
	}

	private TType type;
    private String text;
    
	public Token(TType type) {
		this.type = type;
	}

	public Token(TType type, String text) {
		this.type = type;
		this.text = text;
    }

	public void setType(TType type) {
		this.type = type;
	}

	public TType getType() {
		return type;
	}

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public String toString() {
        String s = "<Token type:" + type;
        if (text != null) {
            s = s + " text:\"" + text + "\"";
        }

        s = s + ">";

        return s;
    }

}
