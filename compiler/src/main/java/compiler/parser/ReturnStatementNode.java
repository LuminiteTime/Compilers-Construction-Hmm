package compiler.parser;

import compiler.lexer.Token;

public class ReturnStatementNode extends AstNode implements StatementNode {
    private final ExpressionNode value; // can be null

    public ReturnStatementNode(Token token, ExpressionNode value) {
        super(token);
        this.value = value;
    }

    public ExpressionNode getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value != null ? "return " + value : "return";
    }
}