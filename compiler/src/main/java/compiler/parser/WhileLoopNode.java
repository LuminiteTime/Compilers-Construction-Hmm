package compiler.parser;

import compiler.lexer.Token;
import java.util.List;

public class WhileLoopNode extends AstNode implements StatementNode {
    private final ExpressionNode condition;
    private final List<AstNode> body;

    public WhileLoopNode(Token token, ExpressionNode condition, List<AstNode> body) {
        super(token);
        this.condition = condition;
        this.body = body;
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public List<AstNode> getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("while ").append(condition).append(" loop\n");
        for (AstNode stmt : body) {
            sb.append("  ").append(stmt).append("\n");
        }
        sb.append("end");
        return sb.toString();
    }
}