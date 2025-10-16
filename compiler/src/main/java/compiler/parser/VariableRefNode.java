package compiler.parser;

import compiler.lexer.Token;

public class VariableRefNode extends AstNode implements ExpressionNode {
    private final String name;

    public VariableRefNode(Token token) {
        super(token);
        this.name = token.getLexeme();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
