package compiler.parser;

import compiler.lexer.Token;

public class AssignmentNode extends AstNode implements StatementNode {
    private final ExpressionNode target;
    private final ExpressionNode value;

    public AssignmentNode(Token token, ExpressionNode target, ExpressionNode value) {
        super(token);
        this.target = target;
        this.value = value;
    }

    public ExpressionNode getTarget() {
        return target;
    }

    public ExpressionNode getValue() {
        return value;
    }

    @Override
    public String toString() {
        return target + " := " + value;
    }
}