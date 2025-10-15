package compiler.parser;

import compiler.lexer.Token;

public class UnaryOpNode extends AstNode implements ExpressionNode {
    private final Operator operator;
    private final ExpressionNode operand;

    public UnaryOpNode(Token token, Operator operator, ExpressionNode operand) {
        super(token);
        this.operator = operator;
        this.operand = operand;
    }

    public Operator getOperator() {
        return operator;
    }

    public ExpressionNode getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return operator.name().toLowerCase() + operand;
    }
}
