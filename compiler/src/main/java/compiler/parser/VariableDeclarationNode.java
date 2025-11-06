package compiler.parser;

import compiler.lexer.Token;

public class VariableDeclarationNode extends AstNode {
    private final String name;
    private final TypeNode type;
    private final ExpressionNode initializer;

    public VariableDeclarationNode(Token token, String name, TypeNode type, ExpressionNode initializer) {
        super(token);
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    public String getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    public ExpressionNode getInitializer() {
        return initializer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("var ").append(name);
        if (type != null) {
            sb.append(" : ").append(type);
        }
        if (initializer != null) {
            sb.append(" := ").append(initializer);
        }
        return sb.toString();
    }
}
