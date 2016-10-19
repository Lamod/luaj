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
		return chunk();
	}

	private Chunk chunk(TType... terminators) throws ParserException {
		Chunk chunk = new Chunk();
		enterBlock(chunk);

        chunk.setStatements(stats(terminators));

		leaveBlock();
		return chunk;
	}

	private Block block(TType... terminators) throws ParserException {
		Block block = new Block();
		enterBlock(block);

        block.setStatements(stats(terminators));

		leaveBlock();
		return block;
	}

	private Stat[] stats(TType... terminators) throws ParserException {
		ArrayList<Stat> statList = new ArrayList<>();
        Stat stat = null;

		while (!testCurrent(TType.EOF)) {
            if (terminators != null && ArrayUtil.contains(terminators, current.getType())) {
                break;
            }
            stat = stat(terminators);
            statList.add(stat);
            tryMatch(TType.SEMICOLON);
            if (stat instanceof LastStat) {
                break;
            }
		}

		return statList.size() > 0 ? statList.toArray(new Stat[statList.size()]) : null;
	}

	private Stat stat(TType... terminators) throws ParserException {
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
                    rs.setExprs(exprs());
                }
                stat = rs;
                break;
            }
			case DO: {
				consume();
				stat = new BlockStat(block(TType.END));
				match(TType.END);
				break;
			}
			case WHILE: {
				consume();
				Expr c = expr();
				match(TType.DO);
				Block b= block(TType.END);
				match(TType.END);
				stat = new WhileStat(c, b);
				break;
			}
			case REPEAT: {
				consume();
				Block b = block(TType.UNTIL);
				match(TType.UNTIL);
				Expr c = expr();
				stat = new RepeatStat(c, b);
				break;
			}
			case IF: {
				consume();
				Expr c = expr();
				match(TType.THEN);
				Block b = block(TType.ELSE, TType.ELSEIF, TType.END);
				
				IfStat s = new IfStat();
				s.append(c, b);

				while (tryMatch(TType.ELSEIF) != null) {
					c = expr();
					match(TType.THEN);
					b = block(TType.ELSE, TType.ELSEIF, TType.END);
					s.append(c, b);
				}

				if (tryMatch(TType.ELSE) != null) {
					s.append(null, block(TType.END));
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
					init = expr();
					match(TType.COMMA);
					limit = expr();
					if (tryMatch(TType.COMMA) != null) {
						step = expr();
					} else {
						step = null;
					}
					match(TType.DO);
					Block b = block(TType.END);
					match(TType.END);
					stat = new NumbericForStat(n, init, limit, step, b);
				} else {
					String[] nl = names();
                    match(TType.IN);
					Expr[] el = exprs();
					match(TType.DO);
					Block b = block(TType.END);
					match(TType.END);
					stat = new GenericForStat(nl, el, b);
				}
				break;
			case FUNCTION:
				stat = funcStat();
				break;
			case LOCAL:
				if (testLookahead(TType.FUNCTION)) {
					consume(); // 'local'
					consume(); // 'function'
					String name = match(TType.NAME).getText();
					FuncExpr func = new FuncExpr(funcBody());

					LocalStat ls = new LocalStat();
					ls.setAccessable(true);
					ls.setNames(new String[]{ name });
					ls.setExprs(new Expr[]{ func });
					stat = ls;
				} else {
					stat = localStat();
				}
				break;
			default:
				stat = exprStat();
		}

		stat.setOwner(this.currentBlock);
		return stat;
	}

	private FuncStat funcStat() throws ParserException {
		match(TType.FUNCTION);

		FuncStat stat = new FuncStat();
		boolean needSelf = false;

		stat.getName().setVar(new Var(match(TType.NAME).getText()));

		ArrayList<String> segments = new ArrayList<>();

		while (tryMatch(TType.DOT) != null) {
			segments.add(match(TType.NAME).getText());
		}
		if (tryMatch(TType.COLON) != null) {
			segments.add(match(TType.NAME).getText());
			needSelf = true;
		}

		stat.getName().setFields(segments.toArray(new String[segments.size()]));
		stat.setBody(funcBody());
		if (needSelf) {
			stat.getBody().setNeedSelf(true);
		}

		return stat;
	}

	private LocalStat localStat() throws ParserException {
		match(TType.LOCAL);

		LocalStat stat = new LocalStat();
		stat.setNames(names());

		if (tryMatch(TType.ASSIGN) == null) {
			return stat;
		}

		stat.setExprs(exprs());

		return stat;
	}

	private Stat exprStat() throws ParserException {
		PrimaryExpr expr = primaryExpr();
		if (expr.isFuncCallExpr()) {
			return new FuncCallStat(expr);
		} else {
			return assignStat(expr);
		}
	}

	private AssignStat assignStat(PrimaryExpr var) throws ParserException {
		AssignStat stat = new AssignStat();

		ArrayList<PrimaryExpr> varList = new ArrayList<>();
		varList.add(var);
		while (tryMatch(TType.COMMA) != null) {
			var = primaryExpr();
			if (!var.isAssignable()) {
				throw new ParserException();
			}
			varList.add(var);
		}
		stat.setVariables(varList.toArray(new PrimaryExpr[varList.size()]));

		match(TType.ASSIGN);
		stat.setValues(exprs());

		return stat;
	}

	private FuncBody funcBody() throws ParserException {
		match(TType.LPARENT);

		FuncBody body = new FuncBody();

		if (tryMatch(TType.RPARENT) == null) {
			body.setParlist(parlist());
			match(TType.RPARENT);
		}

		body.setChunk(chunk(TType.END));
		match(TType.END);

		return body;
	}

	private FuncBody.Parlist parlist() throws ParserException {
		if (testCurrent(TType.DOTS) || testCurrent(TType.NAME)) {
			FuncBody.Parlist parlist = new FuncBody.Parlist();
			if (testCurrent(TType.NAME)) {
				parlist.setParams(names());
			}
			if (tryMatch(TType.DOTS) != null) {
				parlist.setVararg(true);
			}

			return parlist;
		} else {
			throw new ParserException();
		}
	}

	private String[] names() throws ParserException {
		ArrayList<String> names = new ArrayList<>();

		do {
			names.add(match(TType.NAME).getText());
		} while(tryMatch(TType.COMMA) != null);

		return names.toArray(new String[names.size()]);
	}

	private Expr[] exprs() throws ParserException {
		ArrayList<Expr> exps = new ArrayList<>();

		do {
			exps.add(expr());
		} while(tryMatch(TType.COMMA) != null);

		return exps.toArray(new Expr[exps.size()]);
	}

	private Expr expr() throws ParserException {
		return subexpr(0);
	}	

	private Expr subexpr(int limit) throws ParserException {
		Expr expr = null;
		UnaryExpr.Operator uop = UnaryExpr.getOperator(current);
		if (uop != null) {
			consume();
			Expr operand = subexpr(UnaryExpr.OP_PRIORITY);
			UnaryExpr uexpr = new UnaryExpr();
			uexpr.setOperator(uop);
			uexpr.setOperand(operand);
			expr = uexpr;
		} else {
			expr = simpleExpr();
		}

		BinaryExpr.Operator bop = BinaryExpr.getOperator(current);
		while (bop != null && bop.getLeftPriority() > limit) {
			consume();
			expr = new BinaryExpr(expr, bop, subexpr(bop.getRightPriority()));
			bop = BinaryExpr.getOperator(current);
		}

		return expr;
	}

	private Expr simpleExpr() throws ParserException {
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
				return new FuncExpr(funcBody());
			case LBRACE:
				return tableConstructor();
			default:
				return primaryExpr();
		}
	}

	private PrimaryExpr primaryExpr() throws ParserException {
		Expr prefix = null;
		if (testCurrent(TType.NAME)) {
			prefix = new Var(consume().getText());
		} else if (testCurrent(TType.LPARENT)) {
			consume();
			prefix = expr();
			if (prefix instanceof BinaryExpr) {
				((BinaryExpr)prefix).setClosed(true);
			}
			match(TType.RPARENT);
		} else {
			throw new ParserException();
		}

		ArrayList<PrimaryExpr.Segment> segmentList = new ArrayList<>();
		loop:
		while (true) {
			switch (current.getType()) {
				case DOT: {
                    consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(new LiteralString(consume().getText()));
					segmentList.add(seg);

					break;
				}
				case LBRACKET: {
					consume();
					PrimaryExpr.FieldSegment seg = new PrimaryExpr.FieldSegment();
					seg.setKey(expr());
					match(TType.RBRACKET);
					segmentList.add(seg);

					break;
				}
				case COLON: {
					consume();
					PrimaryExpr.MethodSegment seg = new PrimaryExpr.MethodSegment();
					seg.setName(match(TType.NAME).getText());
					seg.setArgs(funcArgs());
					segmentList.add(seg);

					break;
				}
				case LPARENT: case LBRACE: case STRING: {
					PrimaryExpr.ArgsSegment seg = new PrimaryExpr.ArgsSegment();
					seg.setArgs(funcArgs());
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

	private Expr[] funcArgs() throws ParserException {
		switch (current.getType()) {
			case LPARENT: {
				consume();
				Expr[] args = null;
				if (tryMatch(TType.RPARENT) == null) {
					args = exprs();
					match(TType.RPARENT);
				}

				return args;
			}
			case LBRACE:
				return new Expr[] { tableConstructor() };
			case STRING:
				return new Expr[] { new LiteralString(consume().getText()) };
			default:
				throw new ParserException();
		}
	}

	private TableConstructorExpr tableConstructor() throws ParserException {
		match(TType.LBRACE);

		TableConstructorExpr constructor = new TableConstructorExpr();

		if (tryMatch(TType.RBRACE) == null) {
			constructor.setFields(fields());
			match(TType.RBRACE);
		}

		return constructor;
	}

	private TableConstructorExpr.Field[] fields() throws ParserException {
		ArrayList<TableConstructorExpr.Field> fields = new ArrayList<>();

		do {
			switch(current.getType()) {
				case LBRACKET: {
					consume();
					Expr key = expr();
					match(TType.RBRACKET);
					match(TType.ASSIGN);
					Expr value = expr();

					fields.add(new TableConstructorExpr.RecField(key, value));

					break;
				}
				case NAME: {
					if (testLookahead(TType.ASSIGN)) {
						LiteralString key = new LiteralString(consume().getText());
						match(TType.ASSIGN);
						Expr value = expr();

						fields.add(new TableConstructorExpr.RecField(key, value));

						break;
					} else {
						// fallthrough
					}
				}
				default: {
					fields.add(new TableConstructorExpr.ListField(expr()));
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
