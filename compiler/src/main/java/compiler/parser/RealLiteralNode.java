package compiler.parser;

import compiler.lexer.Token;

public class RealLiteralNode extends AstNode implements ExpressionNode {
    private final double value;

    public RealLiteralNode(Token token) {
        super(token);
        this.value = Double.parseDouble(token.getLexeme());
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}