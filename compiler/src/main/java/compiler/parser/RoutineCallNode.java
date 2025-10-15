package compiler.parser;

import compiler.lexer.Token;
import java.util.List;

public class RoutineCallNode extends AstNode implements ExpressionNode, StatementNode {
    private final String name;
    private final List<ExpressionNode> arguments;

    public RoutineCallNode(Token token, String name, List<ExpressionNode> arguments) {
        super(token);
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(arguments.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}