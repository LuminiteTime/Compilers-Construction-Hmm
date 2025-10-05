package compiler.parser;

import compiler.lexer.TokenType;

/**
 * Exception thrown by the parser when a syntax error is encountered.
 * Provides detailed information about the error location and expected tokens.
 */
public class ParserException extends Exception {
    public final String message;
    public final int line;
    public final int column;
    public final TokenType expected; // Optional: what token was expected
    public final TokenType found;    // Optional: what token was found

    /**
     * Constructor with basic error information.
     */
    public ParserException(String message, int line, int column) {
        super(formatMessage(message, line, column, null, null));
        this.message = message;
        this.line = line;
        this.column = column;
        this.expected = null;
        this.found = null;
    }

    /**
     * Constructor with expected and found token information.
     */
    public ParserException(String message, int line, int column, TokenType expected, TokenType found) {
        super(formatMessage(message, line, column, expected, found));
        this.message = message;
        this.line = line;
        this.column = column;
        this.expected = expected;
        this.found = found;
    }

    /**
     * Formats a detailed error message including location and token information.
     */
    private static String formatMessage(String message, int line, int column,
                                      TokenType expected, TokenType found) {
        StringBuilder sb = new StringBuilder();
        sb.append("Parser error at line ").append(line).append(", column ").append(column);
        sb.append(": ").append(message);

        if (expected != null) {
            sb.append(" (expected: ").append(expected);
            if (found != null) {
                sb.append(", found: ").append(found);
            }
            sb.append(")");
        }

        return sb.toString();
    }
}
