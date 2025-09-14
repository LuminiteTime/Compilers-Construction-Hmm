package compiler.lexer;

public class LexerException extends Exception {
    private final int line;
    private final int column;

    public LexerException(String message, int line, int column) {
        super(String.format("Lexical error at line %d, column %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    public LexerException(String message, int line, int column, Throwable cause) {
        super(String.format("Lexical error at line %d, column %d: %s", line, column, message), cause);
        this.line = line;
        this.column = column;
    }
}
