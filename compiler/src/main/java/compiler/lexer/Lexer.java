package compiler.lexer;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Lexer {
    private PushbackReader reader;
    private int currentChar;
    private int line = 1;
    private int column = 1;
    private boolean eofReached = false;
    private Token currentToken; // Store current token for JNI access

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("integer", TokenType.INTEGER);
        KEYWORDS.put("real", TokenType.REAL);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("array", TokenType.ARRAY);
        KEYWORDS.put("record", TokenType.RECORD);
        KEYWORDS.put("end", TokenType.END);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("loop", TokenType.LOOP);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("reverse", TokenType.REVERSE);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("routine", TokenType.ROUTINE);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("xor", TokenType.XOR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("return", TokenType.RETURN);
    }

    public Lexer(Reader reader) throws LexerException {
        this.reader = new PushbackReader(reader);
        try {
            this.currentChar = this.reader.read();
        } catch (IOException e) {
            throw new LexerException("Failed to read from input source", line, column, e);
        }
    }

    public Token nextToken() throws LexerException {
        while (!eofReached) {
            int startLine = line;
            int startColumn = column;

            switch (currentChar) {
                case -1 -> {
                    eofReached = true;
                    return new Token(TokenType.EOF, "", line, column);
                }

                case ' ', '\t', '\r', '\n' -> advance();

                case ';' -> {
                    advance();
                    return new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
                }
                case ',' -> {
                    advance();
                    return new Token(TokenType.COMMA, ",", startLine, startColumn);
                }
                case '(' -> {
                    advance();
                    return new Token(TokenType.LPAREN, "(", startLine, startColumn);
                }
                case ')' -> {
                    advance();
                    return new Token(TokenType.RPAREN, ")", startLine, startColumn);
                }
                case '[' -> {
                    advance();
                    return new Token(TokenType.LBRACKET, "[", startLine, startColumn);
                }
                case ']' -> {
                    advance();
                    return new Token(TokenType.RBRACKET, "]", startLine, startColumn);
                }
                case '+' -> {
                    advance();
                    return new Token(TokenType.PLUS, "+", startLine, startColumn);
                }
                case '*' -> {
                    advance();
                    return new Token(TokenType.MULTIPLY, "*", startLine, startColumn);
                }
                case '%' -> {
                    advance();
                    return new Token(TokenType.MODULO, "%", startLine, startColumn);
                }

                case '-' -> {
                    return scanMinusOrComment(startLine, startColumn);
                }
                case '/' -> {
                    return scanDivideOrComment(startLine, startColumn);
                }
                case '<' -> {
                    return scanLessOrLessEqual(startLine, startColumn);
                }
                case '>' -> {
                    return scanGreaterOrGreaterEqual(startLine, startColumn);
                }
                case '=' -> {
                    return scanEqualOrArrow(startLine, startColumn);
                }
                case ':' -> {
                    return scanColonOrAssign(startLine, startColumn);
                }
                case '.' -> {
                    return scanDotOrRange(startLine, startColumn);
                }
                case '"' -> {
                    return scanStringLiteral(startLine, startColumn);
                }

                default -> {
                    if (Character.isLetter(currentChar) || currentChar == '_') {
                        return scanIdentifierOrKeyword(startLine, startColumn);
                    } else if (Character.isDigit(currentChar) || currentChar == '+' || currentChar == '-') {
                        return scanNumberLiteral(startLine, startColumn);
                    } else {
                        throw new LexerException(
                            String.format("Invalid character: '%c' (ASCII %d)", (char)currentChar, currentChar),
                            line, column);
                    }
                }
            }
        }

        return new Token(TokenType.EOF, "", line, column);
    }

    private Token scanMinusOrComment(int startLine, int startColumn) throws LexerException {
        advance();
        return new Token(TokenType.MINUS, "-", startLine, startColumn);
    }

    private Token scanDivideOrComment(int startLine, int startColumn) throws LexerException {
        advance();

        switch (currentChar) {
            case '/':
                advance();
                while (currentChar != '\n' && currentChar != -1) {
                    advance();
                }
                return nextToken();

            case '*':
                advance();
                while (true) {
                    switch (currentChar) {
                        case -1 -> throw new LexerException("Unterminated multi-line comment", startLine, startColumn);
                        case '*' -> {
                            advance();
                            if (currentChar == '/') {
                                advance();
                                return nextToken();
                            }
                        }
                        default -> advance();
                    }
                }

            case '=':
                advance();
                return new Token(TokenType.NOT_EQUAL, "/=", startLine, startColumn, line, column);

            default:
                return new Token(TokenType.DIVIDE, "/", startLine, startColumn);
        }
    }

    private Token scanLessOrLessEqual(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.LESS_EQUAL, "<=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.LESS, "<", startLine, startColumn);
        }
    }

    private Token scanGreaterOrGreaterEqual(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.GREATER, ">", startLine, startColumn);
        }
    }

    private Token scanEqualOrArrow(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '>') {
            advance();
            return new Token(TokenType.ARROW, "=>", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.EQUAL, "=", startLine, startColumn);
        }
    }

    private Token scanColonOrAssign(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.ASSIGN, ":=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.COLON, ":", startLine, startColumn);
        }
    }

    private Token scanDotOrRange(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '.') {
            advance();
            return new Token(TokenType.RANGE, "..", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.DOT, ".", startLine, startColumn);
        }
    }

    private Token scanStringLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        advance();

        while (currentChar != '"' && currentChar != -1) {
            switch (currentChar) {
                case '\\' -> {
                    lexeme.append((char) currentChar);
                    advance();
                    if (currentChar != -1) {
                        lexeme.append((char) currentChar);
                        advance();
                    }
                }

                case '\n' -> throw new LexerException("Unterminated string literal", startLine, startColumn);

                default -> {
                    lexeme.append((char) currentChar);
                    advance();
                }
            }
        }

        if (currentChar == -1) {
            throw new LexerException("Unterminated string literal", startLine, startColumn);
        }

        lexeme.append('"');
        advance();

        return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startColumn, line, column);
    }

    private Token scanIdentifierOrKeyword(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();

        if (!Character.isLetter(currentChar) && currentChar != '_') {
            throw new LexerException("Invalid identifier start character", line, column);
        }

        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            lexeme.append((char) currentChar);
            advance();
        }

        String identifier = lexeme.toString();

        TokenType type = KEYWORDS.get(identifier);
        return new Token(Objects.requireNonNullElse(type, TokenType.IDENTIFIER),
                         identifier,
                         startLine,
                         startColumn,
                         line,
                         column);
    }

    private Token scanNumberLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        boolean hasDecimalPoint = false;
        boolean hasDigits = false;
        boolean consumedDecimal = false;

        if (currentChar == '+' || currentChar == '-') {
            lexeme.append((char) currentChar);
            advance();
        }

        while (Character.isDigit(currentChar)) {
            lexeme.append((char) currentChar);
            hasDigits = true;
            advance();
        }

        if (currentChar == '.') {
            try {
                int next = reader.read();
                if (next != -1 && Character.isDigit(next)) {
                    hasDecimalPoint = true;
                    consumedDecimal = true;
                    lexeme.append('.');
                    currentChar = next;
                    column++;
                    while (Character.isDigit(currentChar)) {
                        lexeme.append((char) currentChar);
                        hasDigits = true;
                        advance();
                    }
                } else {
                    // not a digit, push back
                    if (next != -1) {
                        reader.unread(next);
                    }
                }
            } catch (IOException e) {
                // ignore, treat as not real
            }
        }

        if (consumedDecimal && currentChar == '.') {
            throw new LexerException("Invalid number format: multiple decimal points", startLine, startColumn);
        }

        if (!hasDigits) {
            throw new LexerException("Invalid number format: no digits found", startLine, startColumn);
        }

        String numberStr = lexeme.toString();
        TokenType type = hasDecimalPoint ? TokenType.REAL_LITERAL : TokenType.INTEGER_LITERAL;

        return new Token(type, numberStr, startLine, startColumn, line, column);
    }

    // Native methods for JNI integration with C++ parser
    public native void initializeParser();
    public native boolean parseInput(String input);

    // JNI-accessible methods that work with current token
    public int nextTokenJNI() throws LexerException {
        currentToken = nextToken();
        return tokenTypeToInt(currentToken.getType());
    }

    public String getLexemeJNI() {
        return currentToken != null ? currentToken.getLexeme() : "";
    }

    public int getTypeJNI() {
        return currentToken != null ? tokenTypeToInt(currentToken.getType()) : 0;
    }

    public int getLineJNI() {
        return currentToken != null ? currentToken.getLine() : 0;
    }

    // Helper method to convert TokenType to int for C++ parser
    private int tokenTypeToInt(TokenType type) {
        return switch (type) {
            case VAR -> 262;
            case TYPE -> 263;
            case IS -> 264;
            case INTEGER -> 265;
            case REAL -> 266;
            case BOOLEAN -> 267;
            case ARRAY -> 268;
            case RECORD -> 269;
            case END -> 270;
            case WHILE -> 271;
            case LOOP -> 272;
            case FOR -> 273;
            case IN -> 274;
            case REVERSE -> 275;
            case IF -> 276;
            case THEN -> 277;
            case ELSE -> 278;
            case PRINT -> 279;
            case ROUTINE -> 280;
            case TRUE -> 281;
            case FALSE -> 282;
            case AND -> 283;
            case OR -> 284;
            case XOR -> 285;
            case NOT -> 286;
            case ASSIGN -> 287;
            case RANGE -> 288;
            case PLUS -> 289;
            case MINUS -> 290;
            case MULTIPLY -> 291;
            case DIVIDE -> 292;
            case MODULO -> 293;
            case LESS -> 294;
            case LESS_EQUAL -> 295;
            case GREATER -> 296;
            case GREATER_EQUAL -> 297;
            case EQUAL -> 298;
            case NOT_EQUAL -> 299;
            case COLON -> 300;
            case SEMICOLON -> 301;
            case COMMA -> 302;
            case DOT -> 303;
            case LPAREN -> 304;
            case RPAREN -> 305;
            case LBRACKET -> 306;
            case RBRACKET -> 307;
            case ARROW -> 308;
            case IDENTIFIER -> 258;
            case INTEGER_LITERAL -> 260;
            case REAL_LITERAL -> 261;
            case STRING_LITERAL -> 259;
            case EOF -> 309;
            default -> 0;
        };
    }

    // Method to set input for JNI parsing
    public void setInputForJNI(String input) throws LexerException {
        this.reader = new PushbackReader(new java.io.StringReader(input));
        this.line = 1;
        this.column = 1;
        this.eofReached = false;
        this.currentToken = null;
        try {
            this.currentChar = this.reader.read();
        } catch (java.io.IOException e) {
            throw new LexerException("Failed to read from input source", line, column, e);
        }
    }

    // Static initializer to load the native library
    static {
        try {
            System.loadLibrary("parser");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Native parser library not available. JNI integration will not work.");
        }
    }

    private void advance() throws LexerException {
        try {
            if (currentChar == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            currentChar = reader.read();
        } catch (IOException e) {
            throw new LexerException("Failed to read next character", line, column, e);
        }
    }
}
