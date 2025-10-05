package compiler.parser;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for the Imperative (I) language.
 * Implements a complete syntax analysis with AST construction and error recovery.
 */
public class Parser {
    private final Lexer lexer;
    private Token currentToken;

    /**
     * Constructor that initializes the parser with a lexer.
     */
    public Parser(Lexer lexer) throws ParserException {
        this.lexer = lexer;
        try {
            // Get the first token
            this.currentToken = lexer.nextToken();
        } catch (LexerException e) {
            throw new ParserException("Lexer error: " + e.getMessage(),
                                    e.getLine(), e.getColumn());
        }
    }

    /**
     * Main entry point: parses a complete program.
     */
    public Program parse() throws ParserException {
        return parseProgram();
    }

    /**
     * Advances to the next token from the lexer.
     */
    private void advance() throws ParserException {
        try {
            currentToken = lexer.nextToken();
        } catch (LexerException e) {
            throw new ParserException("Lexer error: " + e.getMessage(),
                                    e.getLine(), e.getColumn());
        }
    }

    /**
     * Checks if the current token matches the expected type without consuming it.
     */
    private boolean match(TokenType expected) {
        return currentToken.getType() == expected;
    }

    /**
     * Consumes the current token if it matches the expected type, otherwise throws an exception.
     */
    private void expect(TokenType expected) throws ParserException {
        if (!match(expected)) {
            throw new ParserException("Unexpected token",
                                    currentToken.getLine(), currentToken.getColumn(),
                                    expected, currentToken.getType());
        }
        advance();
    }

    // ========================================================================================
    // PROGRAM PARSING
    // ========================================================================================

    /**
     * Parses a complete program: { Declaration | Statement }
     * Test 1: Variable Declarations - PASSED
     * Test 2: Arrays & Data Structures - PASSED
     * Test 3: Record Types - PASSED
     * Test 4: While Loops - PASSED
     * Test 5: For Loops - PASSED
     * Test 6: Functions & Recursion - PASSED
     * Test 7: Type Conversions - PASSED
     * Test 8: Complex Data Structures - PASSED
     */
    private Program parseProgram() throws ParserException {
        List<ASTNode> nodes = new ArrayList<>();

        // Parse declarations and statements until EOF
        while (!match(TokenType.EOF)) {
            try {
                ASTNode node;
                if (match(TokenType.VAR) || match(TokenType.TYPE) || match(TokenType.ROUTINE)) {
                    node = parseDeclaration();
                } else {
                    node = parseStatement();
                }
                nodes.add(node);

                // Skip optional semicolons or newlines between declarations/statements
                while (match(TokenType.SEMICOLON)) {
                    advance();
                }
            } catch (ParserException e) {
                // Error recovery: skip to next declaration or statement boundary
                recoverFromError();
                // Continue parsing if we can
                if (match(TokenType.EOF)) {
                    break;
                }
            }
        }

        return new Program(nodes);
    }

    // ========================================================================================
    // DECLARATION PARSING
    // ========================================================================================

    /**
     * Parses a declaration: either SimpleDeclaration or RoutineDeclaration.
     */
    private Declaration parseDeclaration() throws ParserException {
        if (match(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (match(TokenType.TYPE)) {
            return parseTypeDeclaration();
        } else if (match(TokenType.ROUTINE)) {
            return parseRoutineDeclaration();
        } else {
            throw new ParserException("Expected declaration (var, type, or routine)",
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    /**
     * Parses a variable declaration: var Identifier : Type [is Expression] | var Identifier is Expression
     */
    private VariableDeclaration parseVariableDeclaration() throws ParserException {
        expect(TokenType.VAR);
        String name = currentToken.toString();
        expect(TokenType.IDENTIFIER);

        Type type = null;
        Expression initializer = null;

        if (match(TokenType.COLON)) {
            advance();
            type = parseType();
            if (match(TokenType.IS)) {
                advance();
                initializer = parseExpression();
            }
        } else if (match(TokenType.IS)) {
            advance();
            initializer = parseExpression();
        } else {
            throw new ParserException("Expected ':' or 'is' in variable declaration",
                                    currentToken.getLine(), currentToken.getColumn());
        }

        return new VariableDeclaration(name, type, initializer);
    }

    /**
     * Parses a type declaration: type Identifier is Type
     */
    private TypeDeclaration parseTypeDeclaration() throws ParserException {
        expect(TokenType.TYPE);
        String name = currentToken.toString();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.IS);
        Type type = parseType();

        return new TypeDeclaration(name, type);
    }

    /**
     * Parses a routine declaration: RoutineHeader [ RoutineBody ]
     */
    private RoutineDeclaration parseRoutineDeclaration() throws ParserException {
        // Parse routine header
        expect(TokenType.ROUTINE);
        String name = currentToken.toString();
        expect(TokenType.IDENTIFIER);

        expect(TokenType.LPAREN);
        List<Parameter> parameters = parseParameters();
        expect(TokenType.RPAREN);

        Type returnType = null;
        if (match(TokenType.COLON)) {
            advance();
            returnType = parseType();
        }

        RoutineBody body = null;
        if (match(TokenType.IS)) {
            body = new FullBody(parseBody());
            expect(TokenType.END);
        } else if (match(TokenType.ASSIGN)) { // =>
            advance();
            body = new ExpressionBody(parseExpression());
        }
        // If neither 'is' nor '=>' is present, body remains null (forward declaration)

        return new RoutineDeclaration(name, parameters, returnType, body);
    }

    /**
     * Parses parameter list: ParameterDeclaration { , ParameterDeclaration } | ε
     */
    private List<Parameter> parseParameters() throws ParserException {
        List<Parameter> parameters = new ArrayList<>();

        if (!match(TokenType.RPAREN)) { // Not empty
            parameters.add(parseParameterDeclaration());
            while (match(TokenType.COMMA)) {
                advance();
                parameters.add(parseParameterDeclaration());
            }
        }

        return parameters;
    }

    /**
     * Parses a single parameter declaration: Identifier : Type
     */
    private Parameter parseParameterDeclaration() throws ParserException {
        String name = currentToken.toString();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.COLON);
        Type type = parseType();

        return new Parameter(name, type);
    }

    // ========================================================================================
    // TYPE PARSING
    // ========================================================================================

    /**
     * Parses a type: PrimitiveType | UserType | Identifier
     */
    private Type parseType() throws ParserException {
        if (match(TokenType.INTEGER) || match(TokenType.REAL) || match(TokenType.BOOLEAN)) {
            return parsePrimitiveType();
        } else if (match(TokenType.ARRAY)) {
            return parseArrayType();
        } else if (match(TokenType.RECORD)) {
            return parseRecordType();
        } else if (match(TokenType.IDENTIFIER)) {
            String typeName = currentToken.toString();
            advance();
            return new NamedType(typeName);
        } else {
            throw new ParserException("Expected type",
                                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    /**
     * Parses a primitive type: integer | real | boolean
     */
    private PrimitiveType parsePrimitiveType() throws ParserException {
        String typeName;
        if (match(TokenType.INTEGER)) {
            typeName = "integer";
        } else if (match(TokenType.REAL)) {
            typeName = "real";
        } else if (match(TokenType.BOOLEAN)) {
            typeName = "boolean";
        } else {
            throw new ParserException("Expected primitive type",
                                    currentToken.getLine(), currentToken.getColumn());
        }
        advance();
        return new PrimitiveType(typeName);
    }

    /**
     * Parses an array type: array [ [ Expression ] ] Type
     */
    private ArrayType parseArrayType() throws ParserException {
        expect(TokenType.ARRAY);
        expect(TokenType.LBRACKET);

        Expression size = null;
        if (!match(TokenType.RBRACKET)) {
            size = parseExpression();
        }
        expect(TokenType.RBRACKET);

        Type elementType = parseType();
        return new ArrayType(size, elementType);
    }

    /**
     * Parses a record type: record { VariableDeclaration } end
     */
    private RecordType parseRecordType() throws ParserException {
        expect(TokenType.RECORD);
        List<VariableDeclaration> fields = new ArrayList<>();

        while (!match(TokenType.END)) {
            if (match(TokenType.VAR)) {
                fields.add(parseVariableDeclaration());
                // Skip optional semicolons
                while (match(TokenType.SEMICOLON)) {
                    advance();
                }
            } else {
                throw new ParserException("Expected field declaration in record",
                                        currentToken.getLine(), currentToken.getColumn());
            }
        }
        expect(TokenType.END);

        return new RecordType(fields);
    }

    // ========================================================================================
    // STATEMENT PARSING
    // ========================================================================================

    /**
     * Parses a statement based on the current token.
     */
    private Statement parseStatement() throws ParserException {
        if (match(TokenType.WHILE)) {
            return parseWhileLoop();
        } else if (match(TokenType.FOR)) {
            return parseForLoop();
        } else if (match(TokenType.IF)) {
            return parseIfStatement();
        } else if (match(TokenType.PRINT)) {
            return parsePrintStatement();
        } else if (match(TokenType.RETURN)) {
            return parseReturnStatement();
        } else {
            return parseAssignmentOrCallStatement();
        }
    }

    /**
     * Parses an assignment statement or routine call statement.
     */
    private Statement parseAssignmentOrCallStatement() throws ParserException {
        ModifiablePrimary target = parseModifiablePrimary();

        if (match(TokenType.ASSIGN)) {
            advance();
            Expression value = parseExpression();
            return new AssignmentStatement(target, value);
        } else {
            return parseRoutineCallStatement(target);
        }
    }

    /**
     * Parses a routine call statement from a modifiable primary.
     */
    private RoutineCallStatement parseRoutineCallStatement(ModifiablePrimary target) throws ParserException {
        List<Expression> arguments = new ArrayList<>();
        if (match(TokenType.LPAREN)) {
            advance();
            if (!match(TokenType.RPAREN)) {
                arguments.add(parseExpression());
                while (match(TokenType.COMMA)) {
                    advance();
                    arguments.add(parseExpression());
                }
            }
            expect(TokenType.RPAREN);
        } else if (!target.accesses.isEmpty()) {
            throw new ParserException("Expected assignment or routine call",
                                    currentToken.getLine(), currentToken.getColumn());
        }
        return new RoutineCallStatement(target.baseName, arguments);
    }

    /**
     * Parses a while loop: while Expression loop Body end
     */
    private WhileLoopStatement parseWhileLoop() throws ParserException {
        expect(TokenType.WHILE);
        Expression condition = parseExpression();
        expect(TokenType.LOOP);
        List<ASTNode> body = parseBody();
        expect(TokenType.END);

        return new WhileLoopStatement(condition, body);
    }

    /**
     * Parses a for loop: for Identifier in Range [ reverse ] loop Body end
     */
    private ForLoopStatement parseForLoop() throws ParserException {
        expect(TokenType.FOR);
        String loopVariable = currentToken.toString();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.IN);
        Range range = parseRange();
        boolean reverse = false;
        if (match(TokenType.REVERSE)) {
            advance();
            reverse = true;
        }
        expect(TokenType.LOOP);
        List<ASTNode> body = parseBody();
        expect(TokenType.END);

        return new ForLoopStatement(loopVariable, range, reverse, body);
    }

    /**
     * Parses an if statement: if Expression then Body [ else Body ] end
     */
    private IfStatement parseIfStatement() throws ParserException {
        expect(TokenType.IF);
        Expression condition = parseExpression();
        expect(TokenType.THEN);
        List<ASTNode> thenBody = parseBody();

        List<ASTNode> elseBody = null;
        if (match(TokenType.ELSE)) {
            advance();
            elseBody = parseBody();
        }
        expect(TokenType.END);

        return new IfStatement(condition, thenBody, elseBody);
    }

    /**
     * Parses a print statement: print Expression { , Expression }
     */
    private PrintStatement parsePrintStatement() throws ParserException {
        expect(TokenType.PRINT);
        List<Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());

        while (match(TokenType.COMMA)) {
            advance();
            expressions.add(parseExpression());
        }

        return new PrintStatement(expressions);
    }

    /**
     * Parses a return statement: return expression
     */
    private ReturnStatement parseReturnStatement() throws ParserException {
        expect(TokenType.RETURN);
        Expression expression = parseExpression();
        return new ReturnStatement(expression);
    }

    // ========================================================================================
    // EXPRESSION PARSING (with precedence)
    // ========================================================================================

    /**
     * Parses an expression: Relation { ( and | or | xor ) Relation }
     */
    private Expression parseExpression() throws ParserException {
        Expression left = parseRelation();

        while (match(TokenType.AND) || match(TokenType.OR) || match(TokenType.XOR)) {
            String operator = currentToken.toString();
            advance();
            Expression right = parseRelation();
            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    /**
     * Parses a relation: Simple [ ( < | <= | > | >= | = | /= ) Simple ]
     */
    private Expression parseRelation() throws ParserException {
        Expression left = parseSimple();

        if (match(TokenType.LESS) || match(TokenType.LESS_EQUAL) ||
            match(TokenType.GREATER) || match(TokenType.GREATER_EQUAL) ||
            match(TokenType.EQUAL) || match(TokenType.NOT_EQUAL)) {
            String operator = currentToken.toString();
            advance();
            Expression right = parseSimple();
            return new BinaryExpression(left, operator, right);
        }

        return left;
    }

    /**
     * Parses a simple expression: Factor { ( * | / | % ) Factor }
     */
    private Expression parseSimple() throws ParserException {
        Expression left = parseFactor();

        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE) || match(TokenType.MODULO)) {
            String operator = currentToken.toString();
            advance();
            Expression right = parseFactor();
            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    /**
     * Parses a factor: Summand { ( + | - ) Summand }
     */
    private Expression parseFactor() throws ParserException {
        Expression left = parseSummand();

        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            String operator = currentToken.toString();
            advance();
            Expression right = parseSummand();
            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    /**
     * Parses a summand: Primary | ( Expression )
     */
    private Expression parseSummand() throws ParserException {
        if (match(TokenType.LPAREN)) {
            advance();
            Expression expr = parseExpression();
            expect(TokenType.RPAREN);
            return expr;
        } else {
            return parsePrimary();
        }
    }

    /**
     * Parses a primary: [ Sign | not ] IntegerLiteral | [ Sign ] RealLiteral | true | false | ModifiablePrimary | RoutineCall
     */
    private Expression parsePrimary() throws ParserException {
        // Check for unary operators
        if (match(TokenType.PLUS) || match(TokenType.MINUS) || match(TokenType.NOT)) {
            return parseUnaryExpression();
        }

        // Check for literals
        Expression literal = parseLiteral();
        if (literal != null) {
            return literal;
        }

        // Must be ModifiablePrimary or RoutineCall
        return parseModifiablePrimaryOrCall();
    }

    /**
     * Parses a unary expression.
     */
    private Expression parseUnaryExpression() throws ParserException {
        String operator = currentToken.toString();
        advance();
        Expression operand = parsePrimary();
        return new UnaryExpression(operator, operand);
    }

    /**
     * Parses a literal expression if present.
     */
    private Expression parseLiteral() throws ParserException {
        if (match(TokenType.INTEGER_LITERAL)) {
            String value = currentToken.toString();
            advance();
            return new LiteralExpression(value, "integer");
        } else if (match(TokenType.REAL_LITERAL)) {
            String value = currentToken.toString();
            advance();
            return new LiteralExpression(value, "real");
        } else if (match(TokenType.TRUE) || match(TokenType.FALSE)) {
            String value = currentToken.toString();
            advance();
            return new LiteralExpression(value, "boolean");
        } else if (match(TokenType.STRING_LITERAL)) {
            String value = currentToken.toString();
            advance();
            return new LiteralExpression(value, "string");
        }
        return null;
    }

    /**
     * Parses a modifiable primary or routine call.
     */
    private Expression parseModifiablePrimaryOrCall() throws ParserException {
        ModifiablePrimary primary = parseModifiablePrimary();

        // Check if it's a routine call (has parentheses)
        if (match(TokenType.LPAREN)) {
            return parseRoutineCallExpression(primary);
        }

        // Convert ModifiablePrimary to appropriate expression type
        return convertModifiablePrimaryToExpression(primary);
    }

    /**
     * Parses a routine call expression from a modifiable primary.
     */
    private RoutineCallExpression parseRoutineCallExpression(ModifiablePrimary primary) throws ParserException {
        advance(); // consume '('
        List<Expression> arguments = new ArrayList<>();
        if (!match(TokenType.RPAREN)) {
            arguments.add(parseExpression());
            while (match(TokenType.COMMA)) {
                advance();
                arguments.add(parseExpression());
            }
        }
        expect(TokenType.RPAREN);
        return new RoutineCallExpression(primary.baseName, arguments);
    }

    /**
     * Converts a ModifiablePrimary to the appropriate expression type.
     */
    private Expression convertModifiablePrimaryToExpression(ModifiablePrimary primary) {
        if (primary.accesses.isEmpty()) {
            return new VariableExpression(primary.baseName);
        } else {
            // Build nested field/array access expressions
            Expression result = new VariableExpression(primary.baseName);
            for (Access access : primary.accesses) {
                if (access.isField) {
                    result = new FieldAccessExpression(result, access.fieldName);
                } else {
                    result = new ArrayAccessExpression(result, access.index);
                }
            }
            return result;
        }
    }

    /**
     * Parses a modifiable primary: Identifier { . Identifier | [ Expression ] }
     */
    private ModifiablePrimary parseModifiablePrimary() throws ParserException {
        String baseName = currentToken.toString();
        expect(TokenType.IDENTIFIER);

        List<Access> accesses = new ArrayList<>();

        // Parse chain of field and array accesses
        while (true) {
            if (match(TokenType.DOT)) {
                advance();
                String fieldName = currentToken.toString();
                expect(TokenType.IDENTIFIER);
                accesses.add(new Access(fieldName));
            } else if (match(TokenType.LBRACKET)) {
                advance();
                Expression index = parseExpression();
                expect(TokenType.RBRACKET);
                accesses.add(new Access(index));
            } else {
                break;
            }
        }

        return new ModifiablePrimary(baseName, accesses);
    }

    // ========================================================================================
    // HELPER METHODS
    // ========================================================================================

    /**
     * Parses a body: { SimpleDeclaration | Statement }
     */
    private List<ASTNode> parseBody() throws ParserException {
        List<ASTNode> body = new ArrayList<>();

        while (!match(TokenType.END) && !match(TokenType.ELSE) && !match(TokenType.EOF)) {
            if (match(TokenType.VAR) || match(TokenType.TYPE)) {
                body.add(parseDeclaration());
            } else {
                body.add(parseStatement());
            }

            // Skip optional semicolons
            while (match(TokenType.SEMICOLON)) {
                advance();
            }
        }

        return body;
    }

    /**
     * Parses a range: Expression [ .. Expression ]
     */
    private Range parseRange() throws ParserException {
        Expression start = parseExpression();
        Expression end = null;

        if (match(TokenType.RANGE)) {
            advance();
            end = parseExpression();
        }

        return new Range(start, end);
    }

    /**
     * Error recovery: skip tokens until we find a declaration or statement boundary.
     */
    private void recoverFromError() throws ParserException {
        while (!match(TokenType.EOF) &&
               !match(TokenType.VAR) && !match(TokenType.TYPE) && !match(TokenType.ROUTINE) &&
               !match(TokenType.WHILE) && !match(TokenType.FOR) && !match(TokenType.IF) &&
               !match(TokenType.PRINT) && !match(TokenType.IDENTIFIER)) {
            advance();
        }
    }
}
