package compiler.parser;

import compiler.lexer.Token;

public class IntegerLiteralNode extends AstNode implements ExpressionNode {
    private final int value;

    public IntegerLiteralNode(Token token) {
        super(token);
        this.value = Integer.parseInt(token.getLexeme());
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
