package lamo.luaj.parser;

import lamo.luaj.parser.ast.*;
import lamo.luaj.parser.Token.TType;

import java.io.Reader;
import java.io.Closeable;
import java.util.ArrayList;

public class Parser implements Closeable {

	private Lexer input;
	private Token current, lookahead;

	public Parser(String name, Reader reader) throws ParserException {
		this(new Lexer(name, reader));
	}
	
	public Parser(Lexer input) throws ParserException  {
		this.input = input;
		consume();
	}

	public void close() {
		input.close();
	}

	public Chunk parse() throws ParserException {
		return parseChunk();
	}

	private Chunk parseChunk() throws ParserException {
		Chunk chunk = new Chunk();
		chunk.setStatements(parseStats());

		return chunk;
	}

	private Block parseBlock() throws ParserException {
		Block block = new Block();
		block.setStatements(parseStats());

		return block;
	}

	private Stat[] parseStats() throws ParserException {
		ArrayList<Stat> stats = new ArrayList<Stat>();
		while (!testCurrent(TType.EOF)) {
			stats.add(parseStat());
		}

		return stats.size() > 0 ? stats.toArray(new Stat[stats.size()]) : null;
	}

	private Stat parseStat() throws ParserException {
		if (testCurrent(TType.LOCAL)) {
			if (testLookahead(TType.FUNCTION)) {
				return parseLocalFuncState();
			} else {
				return parseLocalStat();
			}
		} else {
			return null;
		}
	}

	private LocalStat parseLocalStat() throws ParserException {
		match(TType.LOCAL);

		LocalStat stat = new LocalStat();
		stat.setNames(parseNameList());

		if (tryMatch(TType.ASSIGN) == null) {
			return stat;
		}

		stat.setExplist(parseExplist());

		return stat;
	}

	private LocalStat parseLocalFuncState() throws ParserException {
		match(TType.LOCAL);
		match(TType.FUNCTION);

		LocalStat stat = new LocalStat();

		String[] names = new String[]{match(TType.NAME).getText()};
		stat.setNames(names);

		Expr[] explist = new Expr[]{parseFuncBody()};
		stat.setExplist(explist);

		return stat;
	}

	private FuncBody parseFuncBody() throws ParserException {
		match(TType.LPARENT);

		FuncBody body = new FuncBody();

		body.setParlist(parseParlist());

		match(TType.RPARENT);

		body.setBlock(parseBlock());

		return body;
	}

	private FuncBody.Parlist parseParlist() throws ParserException {
		if (testCurrent(TType.DOTS) || testCurrent(TType.NAME)) {
			FuncBody.Parlist parlist = new FuncBody.Parlist();
			ArrayList<String> params = new ArrayList<String>();
			do {
				if (tryMatch(TType.DOTS) != null) {
					parlist.setIsVarargs(true);
					break;
				}
				params.add(match(TType.NAME).getText());
			} while(tryMatch(TType.COMMA) != null);

			if (params.size() > 0) {
				parlist.setParams(params.toArray(new String[params.size()]));
			}

			return parlist;
		} else {
			return null;
		}
	}

	private String[] parseNameList() throws ParserException {
		ArrayList<String> names = new ArrayList<String>();

		do {
			names.add(match(TType.NAME).getText());
		} while(tryMatch(TType.COMMA) != null);

		return names.size() > 0 ? names.toArray(new String[names.size()]) : null;
	}

	private Expr[] parseExplist() throws ParserException {
		ArrayList<Expr> exps = new ArrayList<Expr>();

		do {
			exps.add(parseExpr());
		} while(tryMatch(TType.COMMA) != null);

		return exps.size() > 0 ? exps.toArray(new Expr[exps.size()]) : null;
	}

	private Expr parseExpr() throws ParserException {
		switch(current.getType()) {
			case NIL:
				consume();
				return new Nil();
			case TRUE:
				consume();
				return new True();
			case FALSE:
				consume();
				return new False();
			case NUMBER:
				return new LiteralNumber(consume().getText());
			case STRING:
				return new LiteralString(consume().getText());
			case DOTS:
				break;
			case FUNCTION:
				consume();
				FuncBody body = parseFuncBody();
				break;
			default:
		}

		throw new ParserException();
	}	

	private Token match(TType t) throws ParserException {
		if (current.getType() == t) {
			return consume();
		}

		throw new ParserException();
	}

	private Token tryMatch(TType t) throws ParserException {
		if (current.getType() == t) {
			return consume();
		} else {
			return null;
		}
	}

	private boolean testCurrent(TType t) {
		return current.getType() == t;
	}

	private boolean testLookahead(TType t) {
		return lookahead.getType() == t;
	}

	private Token consume() throws ParserException {
		try {
			Token c = current;
			if (lookahead == null) {
				current = input.next();
				lookahead = input.next();
			} else {
				current = lookahead;
				lookahead = input.next();
			}

			return c;
		} catch (LexerException e) {
			throw new ParserException();
		}
	}

}
