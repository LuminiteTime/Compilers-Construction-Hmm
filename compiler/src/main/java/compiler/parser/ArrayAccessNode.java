package compiler.parser;

import compiler.lexer.Token;

public class ArrayAccessNode extends AstNode implements ExpressionNode {
    private final ExpressionNode array;
    private final ExpressionNode index;

    public ArrayAccessNode(Token token, ExpressionNode array, ExpressionNode index) {
        super(token);
        this.array = array;
        this.index = index;
    }

    public ExpressionNode getArray() {
        return array;
    }

    public ExpressionNode getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return array + "[" + index + "]";
    }
}