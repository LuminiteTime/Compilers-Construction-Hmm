public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;

    public Token(TokenType type, String lexeme, int line, int column, int endLine, int endColumn) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

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

    public String toString() {
        return String.format("%s:%s@%d:%d", type, lexeme, line, column);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        return type == other.type &&
               lexeme.equals(other.lexeme) &&
               line == other.line &&
               column == other.column;
    }

    public int hashCode() {
        return java.util.Objects.hash(type, lexeme, line, column);
    }
}
