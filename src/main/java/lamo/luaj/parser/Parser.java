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
		chunk.setStatements(parseStats().getStats());
		if (testCurrent(TType.RETURN) || testCurrent(TType.BREAK)) {
			chunk.setLastStat(parseLastStat());
			tryMatch(TType.SEMICOLON);
		}

		return chunk;
	}

	private Block parseBlock(TType... terminators) throws ParserException {
		Block block = new Block();
		StatsResult result = parseStats(terminators);
		block.setStatements(result.getStats());
		if (result.terminator == null) {
			if (testCurrent(TType.RETURN) || testCurrent(TType.BREAK)) {
				block.setLastStat(parseLastStat());
				tryMatch(TType.SEMICOLON);
			}
		}

		return block;
	}

	private StatsResult parseStats(TType... terminators) throws ParserException {
		ArrayList<Stat> statList = new ArrayList<Stat>();
		TType terminator = null;

		parseStat:
		while (!testCurrent(TType.EOF)) {
			for (TType t : terminators) {
				if (current.getType() == t) {
					terminator = t;
					break parseStat;
				}
			}
			statList.add(parseStat());
			tryMatch(TType.SEMICOLON);
		}

		Stat[] stats = statList.size() > 0 ? statList.toArray(new Stat[statList.size()]) : null;

		return new StatsResult(stats, terminator);
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
				BlockStat bs = new BlockStat(parseBlock(TType.END));
				match(TType.END);
				return bs;
			}
			case WHILE: {
				consume();
				Expr c = parseExpr();
				match(TType.DO);
				Block b= parseBlock(TType.END);
				match(TType.END);
				return new WhileStat(c, b);
			}
			case REPEAT: {
				consume();
				Block b = parseBlock(TType.UNTIL);
				match(TType.UNTIL);
				Expr c = parseExpr();
				return new RepeatStat(c, b);
			}
			case IF: {
				consume();
				Expr c = parseExpr();
				match(TType.THEN);
				Block b = parseBlock(TType.ELSE, TType.ELSEIF, TType.END);
				
				IfStat s = new IfStat();
				s.append(c, b);

				while (tryMatch(TType.ELSEIF) != null) {
					c = parseExpr();
					match(TType.THEN);
					b = parseBlock(TType.ELSE, TType.ELSEIF, TType.END);
					s.append(c, b);
				}

				if (tryMatch(TType.ELSE) != null) {
					s.append(null, parseBlock(TType.END));
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
					Block b = parseBlock(TType.END);
					match(TType.END);
					return new NumbericForStat(n, init, limit, step, b);
				} else {
					String[] nl = parseNameList();
					Expr[] el = parseExplist();
					match(TType.DO);
					Block b = parseBlock(TType.END);
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
				return parseExprStat();
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

	private Stat parseExprStat() throws ParserException {
		PrimaryExpr expr = parsePrimaryExpr();
		if (expr.isFuncCallExpr()) {
			return new FuncCallStat(expr);
		} else {
			return parseAssignStat(expr);
		}
	}

	private AssignStat parseAssignStat(PrimaryExpr var) throws ParserException {
		AssignStat stat = new AssignStat();

		ArrayList<PrimaryExpr> varList = new ArrayList<PrimaryExpr>();
		varList.add(var);
		while (tryMatch(TType.COMMA) != null) {
			var = parsePrimaryExpr();
			if (var.isFuncCallExpr()) {
				throw new ParserException();
			}
			varList.add(var);
		}
		stat.setVariables(varList.toArray(new PrimaryExpr[varList.size()]));

		if (tryMatch(TType.ASSIGN) != null) {
			stat.setValues(parseExplist());
		}

		return stat;
	}

	private FuncBody parseFuncBody() throws ParserException {
		match(TType.LPARENT);

		FuncBody body = new FuncBody();

		if (tryMatch(TType.RPARENT) == null) {
			body.setParlist(parseParlist());
			match(TType.RPARENT);
		}

		body.setBlock(parseBlock(TType.END));
		match(TType.END);

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
		Expr expr = null;
		UnaryExpr.Operator uop = UnaryExpr.getOperator(current);
		if (uop != null) {
			consume();
			Expr operand = parseExpr();
			UnaryExpr uexpr = new UnaryExpr();
			uexpr.setOperator(uop);
			uexpr.setOperand(operand);
			expr = uexpr;
		} else {
			expr = parseSimpleExpr();
		}

		BinaryExpr.Operator bop = BinaryExpr.getOperator(current);
		while (bop != null) {
			consume();
			expr = BinaryExpr.adjust(expr, bop, parseExpr());
			bop = BinaryExpr.getOperator(current);
		}

		return expr;
	}	

	private Expr parseSimpleExpr() throws ParserException {
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
				return new VarargExpr();
			case FUNCTION:
				consume();
				return new FuncExpr(parseFuncBody());
			case LBRACE:
				return parseTableConstructor();
			default:
				return parsePrimaryExpr();
		}
	}

	private PrimaryExpr parsePrimaryExpr() throws ParserException {
		Expr prefix = null;
		if (testCurrent(TType.NAME)) {
			prefix = new Var(consume().getText());
		} else if (testCurrent(TType.LPARENT)) {
			consume();
			prefix = parseExpr();
			match(TType.RPARENT);
			if (prefix instanceof BinaryExpr) {
				((BinaryExpr)prefix).setClosed(true);
			}
		} else {
			throw new ParserException();
		}

		ArrayList<PrimaryExpr.Segment> segmentList = new ArrayList<PrimaryExpr.Segment>();
		loop:
		while (true) {
			switch (current.getType()) {
				case DOT: {
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(new LiteralString(consume().getText()));
					segmentList.add(seg);

					break;
				}
				case LBRACKET: {
					consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(parseExpr());
					segmentList.add(seg);

					break;
				}
				case COLON: {
					consume();
					PrimaryExpr.FieldAndArgsSegment seg = new PrimaryExpr.FieldAndArgsSegment();
					seg.setKey(match(TType.NAME).getText());
					seg.setArgs(parseFuncArgs());
					segmentList.add(seg);

					break;
				}
				case LPARENT: case LBRACE: case STRING: {
					PrimaryExpr.FuncArgsSegment seg = new PrimaryExpr.FuncArgsSegment();
					seg.setArgs(parseFuncArgs());
					segmentList.add(seg);

					break;
				}
				default:
					break loop;
			}
		}

		PrimaryExpr primaryExpr = new PrimaryExpr();
		primaryExpr.setPrefixExpr(prefix);
		if (segmentList.size() > 0) {
			primaryExpr.setSegments(segmentList.toArray(new PrimaryExpr.Segment[segmentList.size()]));
		}

		return primaryExpr;
	}

	private Expr[] parseFuncArgs() throws ParserException {
		switch (current.getType()) {
			case LPARENT: {
				consume();
				Expr[] args = null;
				if (tryMatch(TType.RPARENT) == null) {
					args = parseExplist();
					match(TType.RPARENT);
				}

				return args;
			}
			case LBRACE:
				return new Expr[] { parseTableConstructor() };
			case STRING:
				return new Expr[] { new LiteralString(consume().getText()) };
			default:
				throw new ParserException();
		}
	}

	private TableConstructorExpr parseTableConstructor() throws ParserException {
		match(TType.LBRACE);

		TableConstructorExpr constructor = new TableConstructorExpr();

		if (tryMatch(TType.RBRACE) == null) {
			constructor.setFields(parseFieldList());
			match(TType.RBRACE);
		}

		return constructor;
	}

	private TableConstructorExpr.Field[] parseFieldList() throws ParserException {
		ArrayList<TableConstructorExpr.Field> fields = new ArrayList<TableConstructorExpr.Field>();

		do {
			switch(current.getType()) {
				case LBRACKET: {
					consume();
					Expr key = parseExpr();
					match(TType.RBRACKET);
					match(TType.ASSIGN);
					Expr value = parseExpr();

					fields.add(new TableConstructorExpr.ExprField(key, value));

					break;
				}
				case NAME: {
					if (testLookahead(TType.ASSIGN)) {
						String key = consume().getText();
						match(TType.ASSIGN);
						Expr value = parseExpr();

						fields.add(new TableConstructorExpr.NameField(key, value));

						break;
					} else {
						// fallthrough
					}
				}
				default: {
					fields.add(new TableConstructorExpr.ListField(parseExpr()));
					break;
				}
			}
		} while (tryMatch(TType.COMMA) != null || tryMatch(TType.SEMICOLON) != null);

		return fields.toArray(new TableConstructorExpr.Field[fields.size()]);
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

	private static class StatsResult {

		private Stat[] stats;
		private TType terminator;

		private StatsResult(Stat[] stats, TType terminator) {
			this.stats = stats;
			this.terminator = terminator;
		}

		private Stat[] getStats() {
			return this.stats;
		}

		private TType getTerminator() {
			return this.terminator;
		}

	}

}
