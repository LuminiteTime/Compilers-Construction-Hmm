package compiler.parser;

import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.lexer.LexerException;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Lexer lexer;
    private Token currentToken;
    private Token peekToken;

    public Parser(Lexer lexer) throws LexerException {
        this.lexer = lexer;
        // Initialize currentToken and peekToken
        this.currentToken = lexer.nextToken();
        this.peekToken = lexer.nextToken();
    }

    private void advance() throws LexerException {
        currentToken = peekToken;
        peekToken = lexer.nextToken();
    }

    private boolean check(TokenType type) {
        return currentToken.getType() == type;
    }

    private boolean match(TokenType type) throws LexerException {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private void expect(TokenType type) throws ParserException, LexerException {
        if (!check(type)) {
            throw new ParserException("Expected " + type + ", but found " + currentToken.getType(),
                                    currentToken.getLine(), currentToken.getColumn());
        }
        try {
            advance();
        } catch (LexerException e) {
            throw new ParserException("Lexer error: " + e.getMessage(), e.getLine(), e.getColumn());
        }
    }

    public ProgramNode parseProgram() throws ParserException, LexerException {
        List<AstNode> declarations = new ArrayList<>();
        Token programToken = currentToken;

        while (!check(TokenType.EOF)) {
            if (check(TokenType.VAR) || check(TokenType.TYPE) || check(TokenType.ROUTINE)) {
                declarations.add(parseSimpleDeclaration());
            } else {
                declarations.add(parseStatement());
            }
        }

        return new ProgramNode(programToken, declarations);
    }

    // Placeholder for other methods - will implement in subsequent steps
    private AstNode parseSimpleDeclaration() throws ParserException, LexerException {
        AstNode decl;
        if (check(TokenType.VAR)) {
            decl = parseVariableDeclaration();
        } else if (check(TokenType.TYPE)) {
            decl = parseTypeDeclaration();
        } else if (check(TokenType.ROUTINE)) {
            decl = parseRoutineDeclaration();
        } else {
            throw new ParserException("Expected var, type, or routine declaration",
                                    currentToken.getLine(), currentToken.getColumn());
        }
        // Consume optional semicolon
        match(TokenType.SEMICOLON);
        return decl;
    }

    private VariableDeclarationNode parseVariableDeclaration() throws ParserException, LexerException {
        Token varToken = currentToken;
        expect(TokenType.VAR);
        Token idToken = currentToken;
        expect(TokenType.IDENTIFIER);
        String name = idToken.getLexeme();

        TypeNode type = null;
        ExpressionNode initializer = null;

        if (match(TokenType.COLON)) {
            type = parseType();
            if (match(TokenType.IS)) {
                initializer = parseExpression();
            }
        } else if (match(TokenType.IS)) {
            initializer = parseExpression();
        } else {
            throw new ParserException("Expected ':' or 'is' in variable declaration",
                                    currentToken.getLine(), currentToken.getColumn());
        }

        return new VariableDeclarationNode(varToken, name, type, initializer);
    }

    private TypeDeclarationNode parseTypeDeclaration() throws ParserException, LexerException {
        Token typeToken = currentToken;
        expect(TokenType.TYPE);
        Token idToken = currentToken;
        expect(TokenType.IDENTIFIER);
        String name = idToken.getLexeme();
        expect(TokenType.IS);
        TypeNode type = parseType();

        return new TypeDeclarationNode(typeToken, name, type);
    }

    private TypeNode parseType() throws ParserException, LexerException {
        if (check(TokenType.INTEGER) || check(TokenType.REAL) || check(TokenType.BOOLEAN)) {
            return parsePrimitiveType();
        } else if (check(TokenType.ARRAY)) {
            return parseArrayType();
        } else if (check(TokenType.RECORD)) {
            return parseRecordType();
        } else if (check(TokenType.IDENTIFIER)) {
            Token idToken = currentToken;
            expect(TokenType.IDENTIFIER);
            return new TypeRefNode(idToken);
        } else {
            throw new ParserException("Expected type, found " + currentToken.getType(),
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    private TypeNode parsePrimitiveType() throws ParserException, LexerException {
        Token token = currentToken;
        if (match(TokenType.INTEGER) || match(TokenType.REAL) || match(TokenType.BOOLEAN)) {
            return new PrimitiveTypeNode(token);
        } else {
            throw new ParserException("Expected primitive type",
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    private TypeNode parseArrayType() throws ParserException, LexerException {
        Token arrayToken = currentToken;
        expect(TokenType.ARRAY);
        expect(TokenType.LBRACKET);

        ExpressionNode size = null;
        if (!check(TokenType.RBRACKET)) {
            size = parseExpression();
        }
        expect(TokenType.RBRACKET);

        TypeNode elementType = parseType();

        return new ArrayTypeNode(arrayToken, size, elementType);
    }

    private TypeNode parseRecordType() throws ParserException, LexerException {
        Token recordToken = currentToken;
        expect(TokenType.RECORD);

        List<VariableDeclarationNode> fields = new ArrayList<>();
        while (!check(TokenType.END)) {
            if (check(TokenType.VAR)) {
                fields.add(parseVariableDeclaration());
            } else {
                throw new ParserException("Expected variable declaration in record",
                                        currentToken.getLine(), currentToken.getColumn());
            }
        }
        expect(TokenType.END);

        return new RecordTypeNode(recordToken, fields);
    }

    private RoutineDeclarationNode parseRoutineDeclaration() throws ParserException, LexerException {
        Token routineToken = currentToken;
        expect(TokenType.ROUTINE);
        expect(TokenType.IDENTIFIER);
        String name = currentToken.getLexeme();
        expect(TokenType.IDENTIFIER);

        expect(TokenType.LPAREN);
        List<ParameterNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            parameters.add(parseParameter());
            while (match(TokenType.COMMA)) {
                parameters.add(parseParameter());
            }
        }
        expect(TokenType.RPAREN);

        TypeNode returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseType();
        }

        List<AstNode> body = null;
        if (match(TokenType.IS)) {
            body = parseBody();
            expect(TokenType.END);
        } else if (match(TokenType.ASSIGN)) {
            // Short form: routine name(...) := expression
            expect(TokenType.GREATER); // => 
            ExpressionNode returnExpr = parseExpression();
            body = List.of(new ReturnStatementNode(routineToken, returnExpr));
        }

        return new RoutineDeclarationNode(routineToken, name, parameters, returnType, body);
    }

    private ParameterNode parseParameter() throws ParserException, LexerException {
        Token paramToken = currentToken;
        expect(TokenType.IDENTIFIER);
        String name = paramToken.getLexeme();
        expect(TokenType.COLON);
        TypeNode type = parseType();

        return new ParameterNode(paramToken, name, type);
    }

    private List<AstNode> parseBody() throws ParserException, LexerException {
        List<AstNode> body = new ArrayList<>();
        while (!check(TokenType.END) && !check(TokenType.ELSE)) {
            if (check(TokenType.VAR) || check(TokenType.TYPE) || check(TokenType.ROUTINE)) {
                body.add(parseSimpleDeclaration());
            } else {
                body.add(parseStatement());
            }
        }
        return body;
    }

    private AstNode parseStatement() throws ParserException, LexerException {
        if (check(TokenType.IDENTIFIER)) {
            // Could be assignment or routine call
            Token idToken = currentToken;
            ExpressionNode modifiable = parseModifiablePrimary();

            if (match(TokenType.ASSIGN)) {
                // Assignment
                ExpressionNode value = parseExpression();
                match(TokenType.SEMICOLON);
                return new AssignmentNode(idToken, modifiable, value);
            } else if (modifiable instanceof VariableRefNode varRef && check(TokenType.LPAREN)) {
                // Routine call
                expect(TokenType.LPAREN);
                List<ExpressionNode> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpression());
                    while (match(TokenType.COMMA)) {
                        args.add(parseExpression());
                    }
                }
                expect(TokenType.RPAREN);
                match(TokenType.SEMICOLON);
                return new RoutineCallNode(idToken, varRef.getName(), args);
            } else {
                throw new ParserException("Expected ':=' or '(' after identifier",
                                        currentToken.getLine(), currentToken.getColumn());
            }
        } else if (check(TokenType.WHILE)) {
            AstNode stmt = parseWhileLoop();
            match(TokenType.SEMICOLON);
            return stmt;
        } else if (check(TokenType.FOR)) {
            AstNode stmt = parseForLoop();
            match(TokenType.SEMICOLON);
            return stmt;
        } else if (check(TokenType.IF)) {
            AstNode stmt = parseIfStatement();
            match(TokenType.SEMICOLON);
            return stmt;
        } else if (check(TokenType.PRINT)) {
            AstNode stmt = parsePrintStatement();
            match(TokenType.SEMICOLON);
            return stmt;
        } else {
            throw new ParserException("Expected statement, found " + currentToken.getType(),
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    private WhileLoopNode parseWhileLoop() throws ParserException, LexerException {
        Token whileToken = currentToken;
        expect(TokenType.WHILE);
        ExpressionNode condition = parseExpression();
        expect(TokenType.LOOP);
        List<AstNode> body = parseBody();
        expect(TokenType.END);

        return new WhileLoopNode(whileToken, condition, body);
    }

    private ForLoopNode parseForLoop() throws ParserException, LexerException {
        Token forToken = currentToken;
        expect(TokenType.FOR);
        Token idToken = currentToken;
        expect(TokenType.IDENTIFIER);
        String variable = idToken.getLexeme();
        expect(TokenType.IN);

        ExpressionNode rangeStart = parseExpression();
        ExpressionNode rangeEnd = null;
        if (match(TokenType.RANGE)) {
            rangeEnd = parseExpression();
        }

        boolean reverse = match(TokenType.REVERSE);

        expect(TokenType.LOOP);
        List<AstNode> body = parseBody();
        expect(TokenType.END);

        return new ForLoopNode(forToken, variable, rangeStart, rangeEnd, reverse, body);
    }

    private IfStatementNode parseIfStatement() throws ParserException, LexerException {
        Token ifToken = currentToken;
        expect(TokenType.IF);
        ExpressionNode condition = parseExpression();
        expect(TokenType.THEN);
        List<AstNode> thenBody = parseBody();

        List<AstNode> elseBody = null;
        if (match(TokenType.ELSE)) {
            elseBody = parseBody();
        }
        expect(TokenType.END);

        return new IfStatementNode(ifToken, condition, thenBody, elseBody);
    }

    private PrintStatementNode parsePrintStatement() throws ParserException, LexerException {
        Token printToken = currentToken;
        expect(TokenType.PRINT);
        List<ExpressionNode> expressions = new ArrayList<>();
        expressions.add(parseExpression());
        while (match(TokenType.COMMA)) {
            expressions.add(parseExpression());
        }

        return new PrintStatementNode(printToken, expressions);
    }

    private ExpressionNode parseExpression() throws ParserException, LexerException {
        ExpressionNode left = parseRelation();
        Token token = currentToken;

        while (true) {
            if (match(TokenType.AND)) {
                ExpressionNode right = parseRelation();
                left = new BinaryOpNode(token, Operator.AND, left, right);
                token = currentToken;
            } else if (match(TokenType.OR)) {
                ExpressionNode right = parseRelation();
                left = new BinaryOpNode(token, Operator.OR, left, right);
                token = currentToken;
            } else if (match(TokenType.XOR)) {
                ExpressionNode right = parseRelation();
                left = new BinaryOpNode(token, Operator.XOR, left, right);
                token = currentToken;
            } else {
                break;
            }
        }

        return left;
    }

    private ExpressionNode parseRelation() throws ParserException, LexerException {
        ExpressionNode left = parseSimple();
        Token token = currentToken;

        if (match(TokenType.LESS)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.LESS, left, right);
        } else if (match(TokenType.LESS_EQUAL)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.LESS_EQUAL, left, right);
        } else if (match(TokenType.GREATER)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.GREATER, left, right);
        } else if (match(TokenType.GREATER_EQUAL)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.GREATER_EQUAL, left, right);
        } else if (match(TokenType.EQUAL)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.EQUAL, left, right);
        } else if (match(TokenType.NOT_EQUAL)) {
            ExpressionNode right = parseSimple();
            return new BinaryOpNode(token, Operator.NOT_EQUAL, left, right);
        }

        return left;
    }

    private ExpressionNode parseSimple() throws ParserException, LexerException {
        ExpressionNode left = parseFactor();
        Token token = currentToken;

        while (true) {
            if (match(TokenType.PLUS)) {
                ExpressionNode right = parseFactor();
                left = new BinaryOpNode(token, Operator.PLUS, left, right);
                token = currentToken;
            } else if (match(TokenType.MINUS)) {
                ExpressionNode right = parseFactor();
                left = new BinaryOpNode(token, Operator.MINUS, left, right);
                token = currentToken;
            } else {
                break;
            }
        }

        return left;
    }

    private ExpressionNode parseFactor() throws ParserException, LexerException {
        ExpressionNode left = parseSummand();
        Token token = currentToken;

        while (true) {
            if (match(TokenType.MULTIPLY)) {
                ExpressionNode right = parseSummand();
                left = new BinaryOpNode(token, Operator.MULTIPLY, left, right);
                token = currentToken;
            } else if (match(TokenType.DIVIDE)) {
                ExpressionNode right = parseSummand();
                left = new BinaryOpNode(token, Operator.DIVIDE, left, right);
                token = currentToken;
            } else if (match(TokenType.MODULO)) {
                ExpressionNode right = parseSummand();
                left = new BinaryOpNode(token, Operator.MODULO, left, right);
                token = currentToken;
            } else {
                break;
            }
        }

        return left;
    }

    private ExpressionNode parseSummand() throws ParserException, LexerException {
        return parsePrimary();
    }

    private ExpressionNode parsePrimary() throws ParserException, LexerException {
        Token token = currentToken;

        if (match(TokenType.INTEGER_LITERAL)) {
            return new IntegerLiteralNode(token);
        } else if (match(TokenType.REAL_LITERAL)) {
            return new RealLiteralNode(token);
        } else if (match(TokenType.TRUE) || match(TokenType.FALSE)) {
            return new BooleanLiteralNode(token);
        } else if (check(TokenType.IDENTIFIER)) {
            ExpressionNode modifiable = parseModifiablePrimary();
            if (modifiable instanceof VariableRefNode varRef && check(TokenType.LPAREN)) {
                // Routine call
                expect(TokenType.LPAREN);
                List<ExpressionNode> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpression());
                    while (match(TokenType.COMMA)) {
                        args.add(parseExpression());
                    }
                }
                expect(TokenType.RPAREN);
                return new RoutineCallNode(token, varRef.getName(), args);
            } else {
                return modifiable;
            }
        } else if (match(TokenType.LPAREN)) {
            ExpressionNode expr = parseExpression();
            expect(TokenType.RPAREN);
            return expr;
        } else if (match(TokenType.PLUS)) {
            ExpressionNode operand = parsePrimary();
            return new UnaryOpNode(token, Operator.UNARY_PLUS, operand);
        } else if (match(TokenType.MINUS)) {
            ExpressionNode operand = parsePrimary();
            return new UnaryOpNode(token, Operator.UNARY_MINUS, operand);
        } else if (match(TokenType.NOT)) {
            ExpressionNode operand = parsePrimary();
            return new UnaryOpNode(token, Operator.NOT, operand);
        } else {
            throw new ParserException("Expected primary expression, found " + currentToken.getType(),
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    private ExpressionNode parseModifiablePrimary() throws ParserException, LexerException {
        Token idToken = currentToken;
        expect(TokenType.IDENTIFIER);
        ExpressionNode node = new VariableRefNode(idToken);

        while (true) {
            if (match(TokenType.DOT)) {
                Token fieldToken = currentToken;
                expect(TokenType.IDENTIFIER);
                String fieldName = fieldToken.getLexeme();
                node = new FieldAccessNode(currentToken, node, fieldName);
            } else if (match(TokenType.LBRACKET)) {
                ExpressionNode index = parseExpression();
                expect(TokenType.RBRACKET);
                node = new ArrayAccessNode(currentToken, node, index);
            } else {
                break;
            }
        }

        return node;
    }
}