import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Lexer for the Imperative (I) language.
 * Implements a finite state machine for lexical analysis with maximal munch principle.
 * Supports Unicode characters, accurate position tracking, and comprehensive error handling.
 */
public class Lexer {
    private final Reader reader;
    private int currentChar;
    private int line = 1;
    private int column = 1;
    private boolean eofReached = false;

    // Keyword lookup table for O(1) recognition
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        // Declaration keywords
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("integer", TokenType.INTEGER);
        KEYWORDS.put("real", TokenType.REAL);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("array", TokenType.ARRAY);
        KEYWORDS.put("record", TokenType.RECORD);
        KEYWORDS.put("end", TokenType.END);

        // Statement keywords
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

        // Expression/Boolean keywords
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("xor", TokenType.XOR);
        KEYWORDS.put("not", TokenType.NOT);

        // Other keywords
        KEYWORDS.put("return", TokenType.RETURN);
    }

    /**
     * Constructs a lexer with the given input source.
     *
     * @param reader The input source reader (typically BufferedReader)
     * @throws LexerException if the reader cannot be read
     */
    public Lexer(Reader reader) throws LexerException {
        this.reader = reader;
        try {
            this.currentChar = reader.read();
        } catch (IOException e) {
            throw new LexerException("Failed to read from input source", line, column, e);
        }
    }

    /**
     * Returns the next token from the input stream.
     * Uses finite state machine approach with maximal munch principle.
     *
     * @return The next token, or EOF token if end of input is reached
     * @throws LexerException if a lexical error is encountered
     */
    public Token nextToken() throws LexerException {
        while (!eofReached) {
            int startLine = line;
            int startColumn = column;

            switch (currentChar) {
                case -1 -> { // EOF
                    eofReached = true;
                    return new Token(TokenType.EOF, "", line, column);
                }

                case ' ', '\t', '\r' -> { // Skip whitespace
                    advance();
                }

                case '\n' -> { // Handle newlines
                    advance();
                }

                // Single-character tokens
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

                // Potential multi-character tokens
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
                    return scanEqualOrNotEqual(startLine, startColumn);
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

    /**
     * Scans a minus token or single-line comment.
     */
    private Token scanMinusOrComment(int startLine, int startColumn) throws LexerException {
        advance(); // consume '-'
        return new Token(TokenType.MINUS, "-", startLine, startColumn);
    }

    /**
     * Scans divide operator or comments (single-line // and multi-line / * ... * /).
     */
    private Token scanDivideOrComment(int startLine, int startColumn) throws LexerException {
        advance(); // consume '/'

        switch (currentChar) {
            case '/':
                // Single-line comment: // ... end of line
                advance(); // consume second '/'
                while (currentChar != '\n' && currentChar != -1) {
                    advance();
                }
                return nextToken(); // Skip comment, return next token

            case '*':
                // Multi-line comment: /* ... */
                advance(); // consume '*'
                while (true) {
                    switch (currentChar) {
                        case -1 -> throw new LexerException("Unterminated multi-line comment", startLine, startColumn);
                        case '*' -> {
                            advance(); // consume '*'
                            if (currentChar == '/') {
                                advance(); // consume '/'
                                return nextToken(); // Skip comment, return next token
                            }
                        }
                        default -> advance();
                    }
                }

            case '=':
                advance(); // consume '='
                return new Token(TokenType.NOT_EQUAL, "/=", startLine, startColumn, line, column);

            default:
                return new Token(TokenType.DIVIDE, "/", startLine, startColumn);
        }
    }

    /**
     * Scans less-than or less-than-or-equal operator.
     */
    private Token scanLessOrLessEqual(int startLine, int startColumn) throws LexerException {
        advance(); // consume '<'
        if (currentChar == '=') {
            advance(); // consume '='
            return new Token(TokenType.LESS_EQUAL, "<=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.LESS, "<", startLine, startColumn);
        }
    }

    /**
     * Scans greater-than or greater-than-or-equal operator.
     */
    private Token scanGreaterOrGreaterEqual(int startLine, int startColumn) throws LexerException {
        advance(); // consume '>'
        if (currentChar == '=') {
            advance(); // consume '='
            return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.GREATER, ">", startLine, startColumn);
        }
    }

    /**
     * Scans equal operator or not-equal operator.
     */
    private Token scanEqualOrNotEqual(int startLine, int startColumn) throws LexerException {
        advance(); // consume '='
        return new Token(TokenType.EQUAL, "=", startLine, startColumn);
    }

    /**
     * Scans colon or assignment operator (:=).
     */
    private Token scanColonOrAssign(int startLine, int startColumn) throws LexerException {
        advance(); // consume ':'
        if (currentChar == '=') {
            advance(); // consume '='
            return new Token(TokenType.ASSIGN, ":=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.COLON, ":", startLine, startColumn);
        }
    }

    /**
     * Scans dot or range operator (..).
     */
    private Token scanDotOrRange(int startLine, int startColumn) throws LexerException {
        advance(); // consume '.'
        if (currentChar == '.') {
            advance(); // consume second '.'
            return new Token(TokenType.RANGE, "..", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.DOT, ".", startLine, startColumn);
        }
    }

    /**
     * Scans string literal with escape sequences.
     */
    private Token scanStringLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        advance(); // consume opening quote

        while (currentChar != '"' && currentChar != -1) {
            switch (currentChar) {
                case '\\' -> {
                    lexeme.append((char) currentChar);
                    advance(); // consume backslash
                    if (currentChar != -1) {
                        lexeme.append((char) currentChar);
                        advance(); // consume escaped character
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
        advance(); // consume closing quote

        return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startColumn, line, column);
    }

    /**
     * Scans identifier or keyword using maximal munch.
     */
    private Token scanIdentifierOrKeyword(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();

        // First character must be letter or underscore
        if (!Character.isLetter(currentChar) && currentChar != '_') {
            throw new LexerException("Invalid identifier start character", line, column);
        }

        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            lexeme.append((char) currentChar);
            advance();
        }

        String identifier = lexeme.toString();

        // Check if it's a keyword
        TokenType type = KEYWORDS.get(identifier);
        if (type != null) {
            return new Token(type, identifier, startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.IDENTIFIER, identifier, startLine, startColumn, line, column);
        }
    }

    /**
     * Scans integer or real number literal.
     */
    private Token scanNumberLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        boolean hasDecimalPoint = false;
        boolean hasDigits = false;

        // Optional sign
        if (currentChar == '+' || currentChar == '-') {
            lexeme.append((char) currentChar);
            advance();
        }

        // Integer part
        while (Character.isDigit(currentChar)) {
            lexeme.append((char) currentChar);
            hasDigits = true;
            advance();
        }

        // Optional decimal part
        if (currentChar == '.') {
            hasDecimalPoint = true;
            lexeme.append((char) currentChar);
            advance();

            // Digits after decimal point
            while (Character.isDigit(currentChar)) {
                lexeme.append((char) currentChar);
                hasDigits = true;
                advance();
            }
        }

        if (!hasDigits) {
            throw new LexerException("Invalid number format: no digits found", startLine, startColumn);
        }

        String numberStr = lexeme.toString();
        TokenType type = hasDecimalPoint ? TokenType.REAL_LITERAL : TokenType.INTEGER_LITERAL;

        return new Token(type, numberStr, startLine, startColumn, line, column);
    }

    /**
     * Advances to the next character, updating line and column counters.
     */
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
