package compiler.parser;

import compiler.lexer.Token;
import java.util.List;

public class RoutineDeclarationNode extends AstNode {
    private final String name;
    private final List<ParameterNode> parameters;
    private final TypeNode returnType; // can be null
    private final List<AstNode> body; // can be null if just header

    public RoutineDeclarationNode(Token token, String name, List<ParameterNode> parameters,
                                  TypeNode returnType, List<AstNode> body) {
        super(token);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public TypeNode getReturnType() {
        return returnType;
    }

    public List<AstNode> getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("routine ").append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i));
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(" : ").append(returnType);
        }
        if (body != null) {
            sb.append(" is\n");
            for (AstNode stmt : body) {
                sb.append("  ").append(stmt).append("\n");
            }
            sb.append("end");
        }
        return sb.toString();
    }
}