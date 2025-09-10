/**
 * Represents a lexical token in the Imperative (I) language.
 * Contains the token type, lexeme (raw text), and position information.
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;

    /**
     * Constructs a token with position information.
     *
     * @param type The token type
     * @param lexeme The raw lexeme string
     * @param line Starting line number (1-based)
     * @param column Starting column number (1-based)
     * @param endLine Ending line number (1-based)
     * @param endColumn Ending column number (1-based)
     */
    public Token(TokenType type, String lexeme, int line, int column, int endLine, int endColumn) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    /**
     * Constructs a token with single-character position (start == end).
     *
     * @param type The token type
     * @param lexeme The raw lexeme string
     * @param line Line number (1-based)
     * @param column Column number (1-based)
     */
    public Token(TokenType type, String lexeme, int line, int column) {
        this(type, lexeme, line, column, line, column);
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    @Override
    public String toString() {
        return String.format("%s:%s@%d:%d", type, lexeme, line, column);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        return type == other.type &&
               lexeme.equals(other.lexeme) &&
               line == other.line &&
               column == other.column;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, lexeme, line, column);
    }
}
