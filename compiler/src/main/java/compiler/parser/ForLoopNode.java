package compiler.parser;

import java.util.List;

import compiler.lexer.Token;

public class ForLoopNode extends AstNode implements StatementNode {
    private final String variable;
    private final ExpressionNode rangeStart;
    private final ExpressionNode rangeEnd; // can be null
    private final boolean reverse;
    private final List<AstNode> body;

    public ForLoopNode(Token token, String variable, ExpressionNode rangeStart, ExpressionNode rangeEnd,
                       boolean reverse, List<AstNode> body) {
        super(token);
        this.variable = variable;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.reverse = reverse;
        this.body = body;
    }

    public String getVariable() {
        return variable;
    }

    public ExpressionNode getRangeStart() {
        return rangeStart;
    }

    public ExpressionNode getRangeEnd() {
        return rangeEnd;
    }

    public boolean isReverse() {
        return reverse;
    }

    public List<AstNode> getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("for ").append(variable).append(" in ");
        sb.append(rangeStart);
        if (rangeEnd != null) {
            sb.append("..").append(rangeEnd);
        }
        if (reverse) {
            sb.append(" reverse");
        }
        sb.append(" loop\n");
        for (AstNode stmt : body) {
            sb.append("  ").append(stmt).append("\n");
        }
        sb.append("end");
        return sb.toString();
    }
}
