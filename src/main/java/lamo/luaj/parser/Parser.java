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
		if (testCurrent(TType.RETURN) || testCurrent(TType.BREAK)) {
			chunk.setLastStat(parseLastStat());
			tryMatch(TType.COLON);
		}

		return chunk;
	}

	private Block parseBlock() throws ParserException {
		Block block = new Block();
		block.setStatements(parseStats());
		if (testCurrent(TType.RETURN) || testCurrent(TType.BREAK)) {
			block.setLastStat(parseLastStat());
			tryMatch(TType.COLON);
		}

		return block;
	}

	private Stat[] parseStats() throws ParserException {
		ArrayList<Stat> stats = new ArrayList<Stat>();
		while (!testCurrent(TType.EOF)) {
			stats.add(parseStat());
			tryMatch(TType.COLON);
		}

		return stats.size() > 0 ? stats.toArray(new Stat[stats.size()]) : null;
	}

	private LastStat parseLastStat() throws ParserException {
		if (testCurrent(TType.BREAK)) {
			return new BreakStat();
		} else {
			match(TType.RETURN);
			return new ReturnStat(parseExplist());
		}
	}

	private Stat parseStat() throws ParserException {
		switch (current.getType()) {
			case DO: {
				consume();
				BlockStat bs = new BlockStat(parseBlock());
				match(TType.END);
				return bs;
			}
			case WHILE: {
				consume();
				Expr c = parseExpr();
				match(TType.DO);
				Block b= parseBlock();
				match(TType.END);
				return new WhileStat(c, b);
			}
			case REPEAT: {
				consume();
				Block b = parseBlock();
				match(TType.UNTIL);
				Expr c = parseExpr();
				return new RepeatStat(c, b);
			}
			case IF: {
				consume();
				Expr c = parseExpr();
				match(TType.THEN);
				Block b = parseBlock();
				
				IfStat s = new IfStat();
				s.append(c, b);

				while (tryMatch(TType.ELSEIF) != null) {
					c = parseExpr();
					match(TType.THEN);
					b = parseBlock();
					s.append(c, b);
				}

				if (tryMatch(TType.ELSE) != null) {
					s.append(null, parseBlock());
				}
				
				match(TType.END);
				return s;
			}
			case FOR:
				consume();
				if (testCurrent(TType.NAME) && testLookahead(TType.ASSIGN)) {
					String n = consume().getText();
					consume(); // '='
					Expr init, limit, step;
					init = parseExpr();
					match(TType.COMMA);
					limit = parseExpr();
					if (tryMatch(TType.COMMA) != null) {
						step = parseExpr();
					} else {
						step = null;
					}
					match(TType.DO);
					Block b = parseBlock();
					match(TType.END);
					return new NumbericForStat(n, init, limit, step, b);
				} else {
					String[] nl = parseNameList();
					Expr[] el = parseExplist();
					match(TType.DO);
					Block b = parseBlock();
					match(TType.END);
					return new GenericForStat(nl, el, b);
				}
			case FUNCTION:
				return parseFuncStat();
			case LOCAL:
				if (testLookahead(TType.FUNCTION)) {
					consume(); // 'local'
					consume(); // 'function'
					String name = match(TType.NAME).getText();
					FuncExpr func = new FuncExpr(parseFuncBody());

					LocalStat stat = new LocalStat();
					stat.setNames(new String[]{ name });
					stat.setExplist(new Expr[]{ func });
					return stat;
				} else {
					return parseLocalStat();
				}
			default:
				throw new ParserException();
		}
	}

	private FuncStat parseFuncStat() throws ParserException {
		match(TType.FUNCTION);

		FuncStat stat = new FuncStat();

		ArrayList<String> segments = new ArrayList<String>();
		segments.add(match(TType.NAME).getText());

		while (tryMatch(TType.COMMA) != null) {
			segments.add(match(TType.NAME).getText());
		}
		if (tryMatch(TType.COMMA) != null) {
			segments.add(match(TType.NAME).getText());
			stat.getName().setNeedSelf(true);
		}

		stat.getName().setSegments(segments.toArray(new String[segments.size()]));
		stat.setBody(parseFuncBody());

		return stat;
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

	private FuncBody parseFuncBody() throws ParserException {
		match(TType.LPARENT);

		FuncBody body = new FuncBody();

		if (tryMatch(TType.RPARENT) == null) {
			body.setParlist(parseParlist());
			match(TType.RPARENT);
		}

		body.setBlock(parseBlock());

		return body;
	}

	private FuncBody.Parlist parseParlist() throws ParserException {
		if (testCurrent(TType.DOTS) || testCurrent(TType.NAME)) {
			FuncBody.Parlist parlist = new FuncBody.Parlist();
			if (testCurrent(TType.NAME)) {
				parlist.setParams(parseNameList());
			}
			if (testCurrent(TType.DOTS)) {
				parlist.setIsVarargs(true);
			}

			return parlist;
		} else {
			throw new ParserException();
		}
	}

	private String[] parseNameList() throws ParserException {
		ArrayList<String> names = new ArrayList<String>();

		do {
			names.add(match(TType.NAME).getText());
		} while(tryMatch(TType.COMMA) != null);

		return names.toArray(new String[names.size()]);
	}

	private Expr[] parseExplist() throws ParserException {
		ArrayList<Expr> exps = new ArrayList<Expr>();

		do {
			exps.add(parseExpr());
		} while(tryMatch(TType.COMMA) != null);

		return exps.toArray(new Expr[exps.size()]);
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
