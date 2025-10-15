package compiler.parser;

import compiler.lexer.Token;

public abstract class AstNode {
    private final Token token; // For position information

    protected AstNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public int getLine() {
        return token.getLine();
    }

    public int getColumn() {
        return token.getColumn();
    }

    public abstract String toString();
}