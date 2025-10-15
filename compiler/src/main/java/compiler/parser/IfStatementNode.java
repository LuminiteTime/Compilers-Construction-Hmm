package compiler.parser;

import java.util.List;

import compiler.lexer.Token;

public class IfStatementNode extends AstNode implements StatementNode {
    private final ExpressionNode condition;
    private final List<AstNode> thenBody;
    private final List<AstNode> elseBody; // can be null

    public IfStatementNode(Token token, ExpressionNode condition, List<AstNode> thenBody, List<AstNode> elseBody) {
        super(token);
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public List<AstNode> getThenBody() {
        return thenBody;
    }

    public List<AstNode> getElseBody() {
        return elseBody;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("if ").append(condition).append(" then\n");
        for (AstNode stmt : thenBody) {
            sb.append("  ").append(stmt).append("\n");
        }
        if (elseBody != null) {
            sb.append("else\n");
            for (AstNode stmt : elseBody) {
                sb.append("  ").append(stmt).append("\n");
            }
        }
        sb.append("end");
        return sb.toString();
    }
}
