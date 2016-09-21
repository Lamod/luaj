package lamo.luaj.parser;

import lamo.luaj.parser.ast.*;
import lamo.luaj.parser.Token.TType;
import lamo.luaj.util.ArrayUtil;

import java.io.Reader;
import java.io.Closeable;
import java.util.ArrayList;

public class Parser implements Closeable {

	private Lexer input;
	private Token current, lookahead;
	private Block currentBlock;

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

	private Chunk parseChunk(TType... terminators) throws ParserException {
		Chunk chunk = new Chunk();
		enterBlock(chunk);

        chunk.setStatements(parseStats(terminators));

		leaveBlock();
		return chunk;
	}

	private Block parseBlock(TType... terminators) throws ParserException {
		Block block = new Block();
		enterBlock(block);

        block.setStatements(parseStats(terminators));

		leaveBlock();
		return block;
	}

	private Stat[] parseStats(TType... terminators) throws ParserException {
		ArrayList<Stat> statList = new ArrayList<>();
        Stat stat = null;

		while (!testCurrent(TType.EOF)) {
            if (terminators != null && ArrayUtil.contains(terminators, current.getType())) {
                break;
            }
            stat = parseStat(terminators);
            statList.add(stat);
            tryMatch(TType.SEMICOLON);
            if (stat instanceof LastStat) {
                break;
            }
		}

		return statList.size() > 0 ? statList.toArray(new Stat[statList.size()]) : null;
	}

	private Stat parseStat(TType... terminators) throws ParserException {
		Stat stat = null;
		switch (current.getType()) {
            case BREAK:
                consume();
                stat = new BreakStat();
                break;
            case RETURN: {
                consume();
                ReturnStat rs = new ReturnStat();
                if (!ArrayUtil.contains(terminators, current.getType())
                        && current.getType() != TType.SEMICOLON) {
                    rs.setExprs(parseExprs());
                }
                stat = rs;
                break;
            }
			case DO: {
				consume();
				stat = new BlockStat(parseBlock(TType.END));
				match(TType.END);
				break;
			}
			case WHILE: {
				consume();
				Expr c = parseExpr();
				match(TType.DO);
				Block b= parseBlock(TType.END);
				match(TType.END);
				stat = new WhileStat(c, b);
				break;
			}
			case REPEAT: {
				consume();
				Block b = parseBlock(TType.UNTIL);
				match(TType.UNTIL);
				Expr c = parseExpr();
				stat = new RepeatStat(c, b);
				break;
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
				stat = s;
				break;
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
					stat = new NumbericForStat(n, init, limit, step, b);
				} else {
					String[] nl = parseNames();
                    match(TType.IN);
					Expr[] el = parseExprs();
					match(TType.DO);
					Block b = parseBlock(TType.END);
					match(TType.END);
					stat = new GenericForStat(nl, el, b);
				}
				break;
			case FUNCTION:
				stat = parseFuncStat();
				break;
			case LOCAL:
				if (testLookahead(TType.FUNCTION)) {
					consume(); // 'local'
					consume(); // 'function'
					String name = match(TType.NAME).getText();
					FuncExpr func = new FuncExpr(parseFuncBody());

					LocalStat ls = new LocalStat();
					ls.setNames(new String[]{ name });
					ls.setExprs(new Expr[]{ func });
					stat = ls;
				} else {
					stat = parseLocalStat();
				}
				break;
			default:
				stat = parseExprStat();
		}

		stat.setOwner(this.currentBlock);
		return stat;
	}

	private FuncStat parseFuncStat() throws ParserException {
		match(TType.FUNCTION);

		FuncStat stat = new FuncStat();
		boolean needSelf = false;

		stat.getName().setVar(new Var(match(TType.NAME).getText()));

		ArrayList<String> segments = new ArrayList<>();

		while (tryMatch(TType.COMMA) != null) {
			segments.add(match(TType.NAME).getText());
		}
		if (tryMatch(TType.COLON) != null) {
			segments.add(match(TType.NAME).getText());
			needSelf = true;
		}

		stat.getName().setFields(segments.toArray(new String[segments.size()]));
		stat.setBody(parseFuncBody());
		if (needSelf) {
			stat.getBody().setNeedSelf(true);
		}

		return stat;
	}

	private LocalStat parseLocalStat() throws ParserException {
		match(TType.LOCAL);

		LocalStat stat = new LocalStat();
		stat.setNames(parseNames());

		if (tryMatch(TType.ASSIGN) == null) {
			return stat;
		}

		stat.setExprs(parseExprs());

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

		ArrayList<PrimaryExpr> varList = new ArrayList<>();
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
			stat.setValues(parseExprs());
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

		body.setChunk(parseChunk(TType.END));
		match(TType.END);

		return body;
	}

	private FuncBody.Parlist parseParlist() throws ParserException {
		if (testCurrent(TType.DOTS) || testCurrent(TType.NAME)) {
			FuncBody.Parlist parlist = new FuncBody.Parlist();
			if (testCurrent(TType.NAME)) {
				parlist.setParams(parseNames());
			}
			if (tryMatch(TType.DOTS) != null) {
				parlist.setIsVarargs(true);
			}

			return parlist;
		} else {
			throw new ParserException();
		}
	}

	private String[] parseNames() throws ParserException {
		ArrayList<String> names = new ArrayList<>();

		do {
			names.add(match(TType.NAME).getText());
		} while(tryMatch(TType.COMMA) != null);

		return names.toArray(new String[names.size()]);
	}

	private Expr[] parseExprs() throws ParserException {
		ArrayList<Expr> exps = new ArrayList<>();

		do {
			exps.add(parseExpr());
		} while(tryMatch(TType.COMMA) != null);

		return exps.toArray(new Expr[exps.size()]);
	}

	private Expr parseExpr() throws ParserException {
		return parseSubexpr(0);
	}	

	private Expr parseSubexpr(int limit) throws ParserException {
		Expr expr = null;
		UnaryExpr.Operator uop = UnaryExpr.getOperator(current);
		if (uop != null) {
			consume();
			Expr operand = parseSubexpr(UnaryExpr.OP_PRIORITY);
			UnaryExpr uexpr = new UnaryExpr();
			uexpr.setOperator(uop);
			uexpr.setOperand(operand);
			expr = uexpr;
		} else {
			expr = parseSimpleExpr();
		}

		BinaryExpr.Operator bop = BinaryExpr.getOperator(current);
		while (bop != null && bop.getLeftPriority() > limit) {
			consume();
			expr = new BinaryExpr(expr, bop, parseSubexpr(bop.getRightPriority()));
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
				consume();
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
			if (prefix instanceof BinaryExpr) {
				((BinaryExpr)prefix).setClosed(true);
			}
			match(TType.RPARENT);
		} else {
			throw new ParserException();
		}

		ArrayList<PrimaryExpr.Segment> segmentList = new ArrayList<>();
		boolean needSelf = false;
		loop:
		while (true) {
			switch (current.getType()) {
				case DOT: {
					if (needSelf) {
						throw new ParserException();
					}
                    consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(new LiteralString(consume().getText()));
					segmentList.add(seg);

					break;
				}
				case LBRACKET: {
					if (needSelf) {
						throw new ParserException();
					}
					consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(parseExpr());
					match(TType.RBRACKET);
					segmentList.add(seg);

					break;
				}
				case COLON: {
					if (needSelf) {
						throw new ParserException();
					}
					consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(new LiteralString(consume().getText()));
					segmentList.add(seg);
					needSelf = true;

					break;
				}
				case LPARENT: case LBRACE: case STRING: {
					PrimaryExpr.FuncArgsSegment seg = new PrimaryExpr.FuncArgsSegment();
					seg.setArgs(parseFuncArgs());
					if (needSelf) {
						seg.setNeedSelf(true);
						needSelf = false;
					}
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
					args = parseExprs();
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
			constructor.setFields(parseFields());
			match(TType.RBRACE);
		}

		return constructor;
	}

	private TableConstructorExpr.Field[] parseFields() throws ParserException {
		ArrayList<TableConstructorExpr.Field> fields = new ArrayList<>();

		do {
			switch(current.getType()) {
				case LBRACKET: {
					consume();
					Expr key = parseExpr();
					match(TType.RBRACKET);
					match(TType.ASSIGN);
					Expr value = parseExpr();

					fields.add(new TableConstructorExpr.RecField(key, value));

					break;
				}
				case NAME: {
					if (testLookahead(TType.ASSIGN)) {
						LiteralString key = new LiteralString(consume().getText());
						match(TType.ASSIGN);
						Expr value = parseExpr();

						fields.add(new TableConstructorExpr.RecField(key, value));

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
		} while ((tryMatch(TType.COMMA) != null || tryMatch(TType.SEMICOLON) != null) && !testCurrent(TType.RBRACE));

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

	private void enterBlock(Block block) {
		int level = this.currentBlock != null ? this.currentBlock.getLevel() + 1 : 0;
		block.setLevel(level);
		block.setParent(this.currentBlock);
		this.currentBlock = block;
	}

	private void leaveBlock() {
		this.currentBlock = this.currentBlock.getParent();
	}

}
