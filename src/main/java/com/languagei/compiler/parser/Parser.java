package com.languagei.compiler.parser;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.lexer.Lexer;
import com.languagei.compiler.lexer.Token;
import com.languagei.compiler.lexer.TokenType;
import com.languagei.compiler.lexer.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for Language I
 */
public class Parser {
    private final Lexer lexer;
    private Token current;
    private Token previous;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    /**
     * Parse a complete program
     */
    public ProgramNode parse() {
        Position pos = current.getPosition();
        ProgramNode program = new ProgramNode(pos);

        while (!check(TokenType.EOF)) {
            try {
                if (check(TokenType.ROUTINE)) {
                    program.addDeclaration(parseRoutineDeclaration());
                } else if (check(TokenType.VAR) || check(TokenType.TYPE)) {
                    program.addDeclaration(parseSimpleDeclaration());
                } else if (!check(TokenType.EOF)) {
                    program.addStatement(parseStatement());
                } else {
                    break;
                }

                // Skip separators
                while (match(TokenType.NEWLINE, TokenType.SEMICOLON)) {
                    // consume separators
                }
            } catch (ParseException e) {
                if (!synchronize()) {
                    // If we can't synchronize, break to avoid infinite loop
                    break;
                }
            }
        }

        return program;
    }

    // Declarations

    private ASTNode parseSimpleDeclaration() {
        if (check(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (check(TokenType.TYPE)) {
            return parseTypeDeclaration();
        }
        throw error("Expected variable or type declaration");
    }

    private VariableDeclarationNode parseVariableDeclaration() {
        Position pos = current.getPosition();
        consume(TokenType.VAR, "Expected 'var'");
        String name = consume(TokenType.IDENTIFIER, "Expected identifier").getLexeme();

        ASTNode type = null;
        ASTNode initializer = null;

        if (match(TokenType.COLON)) {
            type = parseType();
            if (match(TokenType.IS)) {
                initializer = parseExpression();
            }
        } else if (match(TokenType.IS)) {
            initializer = parseExpression();
        } else {
            throw error("Expected ':' or 'is' in variable declaration");
        }

        return new VariableDeclarationNode(pos, name, type, initializer);
    }

    private TypeDeclarationNode parseTypeDeclaration() {
        Position pos = current.getPosition();
        consume(TokenType.TYPE, "Expected 'type'");
        String name = consume(TokenType.IDENTIFIER, "Expected type name").getLexeme();
        consume(TokenType.IS, "Expected 'is'");
        ASTNode type = parseType();
        return new TypeDeclarationNode(pos, name, type);
    }

    private RoutineDeclarationNode parseRoutineDeclaration() {
        Position pos = current.getPosition();
        consume(TokenType.ROUTINE, "Expected 'routine'");
        String name = consume(TokenType.IDENTIFIER, "Expected routine name").getLexeme();

        consume(TokenType.LPAREN, "Expected '('");
        List<ParameterNode> parameters = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Position paramPos = current.getPosition();
                String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name").getLexeme();
                consume(TokenType.COLON, "Expected ':'");
                ASTNode paramType = parseType();
                parameters.add(new ParameterNode(paramPos, paramName, paramType));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')'");

        ASTNode returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseType();
        }

        BlockNode body = null;
        if (match(TokenType.IS)) {
            body = parseBlock();
            consume(TokenType.END, "Expected 'end'");
        } else if (match(TokenType.ARROW)) {
            // Expression body (shorthand for function)
            ASTNode expr = parseExpression();
            body = new BlockNode(expr.getPosition());
            body.addStatement(new ReturnStatementNode(expr.getPosition(), expr));
        }

        return new RoutineDeclarationNode(pos, name, parameters, returnType, body);
    }

    // Types

    private ASTNode parseType() {
        if (match(TokenType.INTEGER_TYPE)) {
            return new PrimitiveTypeNode(previous.getPosition(), PrimitiveTypeNode.PrimitiveType.INTEGER);
        }
        if (match(TokenType.REAL_TYPE)) {
            return new PrimitiveTypeNode(previous.getPosition(), PrimitiveTypeNode.PrimitiveType.REAL);
        }
        if (match(TokenType.BOOLEAN_TYPE)) {
            return new PrimitiveTypeNode(previous.getPosition(), PrimitiveTypeNode.PrimitiveType.BOOLEAN);
        }
        if (match(TokenType.ARRAY)) {
            Position pos = previous.getPosition();
            ASTNode size = null;
            if (match(TokenType.LBRACKET)) {
                if (!check(TokenType.RBRACKET)) {
                    size = parseExpression();
                }
                consume(TokenType.RBRACKET, "Expected ']'");
            }
            ASTNode elementType = parseType();
            return new ArrayTypeNode(pos, size, elementType);
        }
        if (match(TokenType.RECORD)) {
            Position pos = previous.getPosition();
            RecordTypeNode record = new RecordTypeNode(pos);
            while (!check(TokenType.END) && !check(TokenType.EOF)) {
                if (check(TokenType.VAR)) {
                    VariableDeclarationNode field = parseVariableDeclaration();
                    record.addField(field);
                }
                while (match(TokenType.NEWLINE, TokenType.SEMICOLON)) {
                    // consume separators
                }
            }
            consume(TokenType.END, "Expected 'end'");
            return record;
        }
        if (check(TokenType.IDENTIFIER)) {
            Token id = advance();
            return new TypeReferenceNode(id.getPosition(), id.getLexeme());
        }
        throw error("Expected type");
    }

    // Statements

    private BlockNode parseBlock() {
        Position pos = current.getPosition();
        BlockNode block = new BlockNode(pos);

        while (!check(TokenType.END) && !check(TokenType.EOF) && !check(TokenType.ELSE)) {
            if (check(TokenType.VAR) || check(TokenType.TYPE)) {
                block.addStatement(parseSimpleDeclaration());
            } else {
                block.addStatement(parseStatement());
            }
            while (match(TokenType.NEWLINE, TokenType.SEMICOLON)) {
                // consume separators
            }
        }

        return block;
    }

    private ASTNode parseStatement() {
        if (match(TokenType.IF)) {
            return parseIfStatement();
        }
        if (match(TokenType.WHILE)) {
            return parseWhileLoop();
        }
        if (match(TokenType.FOR)) {
            return parseForLoop();
        }
        if (match(TokenType.RETURN)) {
            return parseReturnStatement();
        }
        if (match(TokenType.PRINT)) {
            return parsePrintStatement();
        }

        // Try to parse as assignment or routine call
        ASTNode expr = parsePrimary();
        expr = parsePostfix(expr);

        if (match(TokenType.ASSIGN)) {
            ASTNode value = parseExpression();
            return new AssignmentNode(expr.getPosition(), expr, value);
        }

        // If it's a routine call, it's a statement by itself
        if (expr instanceof RoutineCallNode) {
            return expr;
        }

        throw error("Expected statement");
    }

    private IfStatementNode parseIfStatement() {
        Position pos = previous.getPosition();
        ASTNode condition = parseExpression();
        consume(TokenType.THEN, "Expected 'then'");
        BlockNode thenBlock = parseBlock();

        BlockNode elseBlock = null;
        if (match(TokenType.ELSE)) {
            elseBlock = parseBlock();
        }

        consume(TokenType.END, "Expected 'end'");
        return new IfStatementNode(pos, condition, thenBlock, elseBlock);
    }

    private WhileLoopNode parseWhileLoop() {
        Position pos = previous.getPosition();
        ASTNode condition = parseExpression();
        consume(TokenType.LOOP, "Expected 'loop'");
        BlockNode body = parseBlock();
        consume(TokenType.END, "Expected 'end'");
        return new WhileLoopNode(pos, condition, body);
    }

    private ForLoopNode parseForLoop() {
        Position pos = previous.getPosition();
        String variable = consume(TokenType.IDENTIFIER, "Expected loop variable").getLexeme();
        consume(TokenType.IN, "Expected 'in'");

        ASTNode expr = parseRelation();
        
        ASTNode rangeStart = null, rangeEnd = null, arrayExpr = null;
        
        if (match(TokenType.RANGE)) {
            // It was a range start
            rangeStart = expr;
            rangeEnd = parseRelation();
        } else {
            // It's an array expression
            arrayExpr = expr;
        }

        boolean reverse = match(TokenType.REVERSE);
        consume(TokenType.LOOP, "Expected 'loop'");
        BlockNode body = parseBlock();
        consume(TokenType.END, "Expected 'end'");

        return new ForLoopNode(pos, variable, rangeStart, rangeEnd, arrayExpr, reverse, body);
    }

    private ReturnStatementNode parseReturnStatement() {
        Position pos = previous.getPosition();
        ASTNode value = null;
        if (!check(TokenType.END) && !check(TokenType.EOF) && !check(TokenType.NEWLINE) && !check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        return new ReturnStatementNode(pos, value);
    }

    private PrintStatementNode parsePrintStatement() {
        Position pos = previous.getPosition();
        PrintStatementNode print = new PrintStatementNode(pos);
        do {
            print.addExpression(parseExpression());
        } while (match(TokenType.COMMA));
        return print;
    }

    // Expressions - Following operator precedence

    private ASTNode parseExpression() {
        return parseLogicalOr();
    }

    private ASTNode parseLogicalOr() {
        ASTNode expr = parseLogicalXor();

        while (match(TokenType.OR)) {
            Token op = previous;
            ASTNode right = parseLogicalXor();
            expr = new BinaryExpressionNode(op.getPosition(), expr, BinaryExpressionNode.Operator.OR, right);
        }

        return expr;
    }

    private ASTNode parseLogicalXor() {
        ASTNode expr = parseLogicalAnd();

        while (match(TokenType.XOR)) {
            Token op = previous;
            ASTNode right = parseLogicalAnd();
            expr = new BinaryExpressionNode(op.getPosition(), expr, BinaryExpressionNode.Operator.XOR, right);
        }

        return expr;
    }

    private ASTNode parseLogicalAnd() {
        ASTNode expr = parseRelation();

        while (match(TokenType.AND)) {
            Token op = previous;
            ASTNode right = parseRelation();
            expr = new BinaryExpressionNode(op.getPosition(), expr, BinaryExpressionNode.Operator.AND, right);
        }

        return expr;
    }

    private ASTNode parseRelation() {
        ASTNode expr = parseAdditive();

        if (check(TokenType.LT) || check(TokenType.LE) || check(TokenType.GT) || 
            check(TokenType.GE) || check(TokenType.EQ) || check(TokenType.NE)) {
            Token op = advance();
            ASTNode right = parseAdditive();

            BinaryExpressionNode.Operator operator = switch(op.getType()) {
                case LT -> BinaryExpressionNode.Operator.LT;
                case LE -> BinaryExpressionNode.Operator.LE;
                case GT -> BinaryExpressionNode.Operator.GT;
                case GE -> BinaryExpressionNode.Operator.GE;
                case EQ -> BinaryExpressionNode.Operator.EQ;
                case NE -> BinaryExpressionNode.Operator.NE;
                default -> throw new RuntimeException("Invalid operator");
            };

            expr = new BinaryExpressionNode(op.getPosition(), expr, operator, right);
        }

        return expr;
    }

    private ASTNode parseAdditive() {
        ASTNode expr = parseMultiplicative();

        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            Token op = advance();
            ASTNode right = parseMultiplicative();

            BinaryExpressionNode.Operator operator = op.getType() == TokenType.PLUS ? 
                BinaryExpressionNode.Operator.PLUS : BinaryExpressionNode.Operator.MINUS;

            expr = new BinaryExpressionNode(op.getPosition(), expr, operator, right);
        }

        return expr;
    }

    private ASTNode parseMultiplicative() {
        ASTNode expr = parseUnary();

        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            Token op = advance();
            ASTNode right = parseUnary();

            BinaryExpressionNode.Operator operator = switch(op.getType()) {
                case STAR -> BinaryExpressionNode.Operator.MULTIPLY;
                case SLASH -> BinaryExpressionNode.Operator.DIVIDE;
                case PERCENT -> BinaryExpressionNode.Operator.MODULO;
                default -> throw new RuntimeException("Invalid operator");
            };

            expr = new BinaryExpressionNode(op.getPosition(), expr, operator, right);
        }

        return expr;
    }

    private ASTNode parseUnary() {
        if (match(TokenType.PLUS)) {
            Token op = previous;
            ASTNode expr = parseUnary();
            return new UnaryExpressionNode(op.getPosition(), UnaryExpressionNode.Operator.PLUS, expr);
        }
        if (match(TokenType.MINUS)) {
            Token op = previous;
            ASTNode expr = parseUnary();
            return new UnaryExpressionNode(op.getPosition(), UnaryExpressionNode.Operator.MINUS, expr);
        }
        if (match(TokenType.NOT)) {
            Token op = previous;
            ASTNode expr = parseUnary();
            return new UnaryExpressionNode(op.getPosition(), UnaryExpressionNode.Operator.NOT, expr);
        }

        return parsePostfix(parsePrimary());
    }

    private ASTNode parsePrimary() {
        if (match(TokenType.TRUE)) {
            return new LiteralNode(previous.getPosition(), true);
        }
        if (match(TokenType.FALSE)) {
            return new LiteralNode(previous.getPosition(), false);
        }
        if (match(TokenType.INTEGER_LITERAL)) {
            return new LiteralNode(previous.getPosition(), previous.getLiteral());
        }
        if (match(TokenType.REAL_LITERAL)) {
            return new LiteralNode(previous.getPosition(), previous.getLiteral());
        }
        if (match(TokenType.IDENTIFIER)) {
            return new IdentifierNode(previous.getPosition(), previous.getLexeme());
        }
        if (match(TokenType.LPAREN)) {
            ASTNode expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')'");
            return expr;
        }

        throw error("Expected primary expression");
    }

    private ASTNode parsePostfix(ASTNode expr) {
        while (true) {
            if (match(TokenType.LBRACKET)) {
                ASTNode index = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']'");
                expr = new ArrayAccessNode(expr.getPosition(), expr, index);
            } else if (match(TokenType.DOT)) {
                String fieldName = consume(TokenType.IDENTIFIER, "Expected field name").getLexeme();
                expr = new RecordAccessNode(expr.getPosition(), expr, fieldName);
            } else if (expr instanceof IdentifierNode && match(TokenType.LPAREN)) {
                RoutineCallNode call = new RoutineCallNode(expr.getPosition(), ((IdentifierNode)expr).getName());
                if (!check(TokenType.RPAREN)) {
                    do {
                        call.addArgument(parseExpression());
                    } while (match(TokenType.COMMA));
                }
                consume(TokenType.RPAREN, "Expected ')'");
                expr = call;
            } else {
                break;
            }
        }
        return expr;
    }

    // Helper methods

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return current.getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            previous = current;
            current = lexer.nextToken();
        }
        return previous;
    }

    private boolean isAtEnd() {
        return current.getType() == TokenType.EOF;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(message);
    }

    private ParseException error(String message) {
        return new ParseException(message + " at " + current.getPosition());
    }

    private boolean synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous.getType() == TokenType.SEMICOLON || previous.getType() == TokenType.NEWLINE) {
                return true;
            }
            switch (current.getType()) {
                case VAR, TYPE, ROUTINE, IF, WHILE, FOR, RETURN, PRINT:
                    return true;
                case EOF:
                    return false; // Reached end of file, can't recover
                default:
                    break;
            }
            advance();
        }
        return false; // End of file reached
    }
}

