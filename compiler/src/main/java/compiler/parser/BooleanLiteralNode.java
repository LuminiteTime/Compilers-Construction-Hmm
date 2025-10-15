package compiler.parser;

import compiler.lexer.Token;

public class BooleanLiteralNode extends AstNode implements ExpressionNode {
    private final boolean value;

    public BooleanLiteralNode(Token token) {
        super(token);
        this.value = token.getType() == compiler.lexer.TokenType.TRUE;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}