package com.languagei.compiler.lexer;

/**
 * Represents a lexical token
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final Object literal;
    private final Position position;

    public Token(TokenType type, String lexeme, Object literal, Position position) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.position = position;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public Object getLiteral() {
        return literal;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s at %s", type, lexeme, literal, position);
    }
}

