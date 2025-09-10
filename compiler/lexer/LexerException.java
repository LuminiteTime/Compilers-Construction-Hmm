/**
 * Exception thrown by the lexer when encountering lexical errors.
 * Contains detailed information about the error location and description.
 */
public class LexerException extends Exception {
    private final int line;
    private final int column;

    /**
     * Constructs a lexer exception with position information.
     *
     * @param message The error message
     * @param line The line number where the error occurred (1-based)
     * @param column The column number where the error occurred (1-based)
     */
    public LexerException(String message, int line, int column) {
        super(String.format("Lexical error at line %d, column %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a lexer exception with position information and cause.
     *
     * @param message The error message
     * @param line The line number where the error occurred (1-based)
     * @param column The column number where the error occurred (1-based)
     * @param cause The underlying cause of the exception
     */
    public LexerException(String message, int line, int column, Throwable cause) {
        super(String.format("Lexical error at line %d, column %d: %s", line, column, message), cause);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
