package compiler.parser;

import compiler.lexer.Token;

public class BinaryOpNode extends AstNode implements ExpressionNode {
    private final Operator operator;
    private final ExpressionNode left;
    private final ExpressionNode right;

    public BinaryOpNode(Token token, Operator operator, ExpressionNode left, ExpressionNode right) {
        super(token);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    @Override
    public String toString() {
        return "(" + left + " " + operator.name().toLowerCase() + " " + right + ")";
    }
}