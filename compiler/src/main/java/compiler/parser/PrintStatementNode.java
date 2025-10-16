package compiler.parser;

import java.util.List;

import compiler.lexer.Token;

public class PrintStatementNode extends AstNode implements StatementNode {
    private final List<ExpressionNode> expressions;

    public PrintStatementNode(Token token, List<ExpressionNode> expressions) {
        super(token);
        this.expressions = expressions;
    }

    public List<ExpressionNode> getExpressions() {
        return expressions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("print ");
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(expressions.get(i));
        }
        return sb.toString();
    }
}
