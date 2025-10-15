package compiler.parser;

import compiler.lexer.Token;

public class TypeRefNode extends TypeNode {
    private final String name;

    public TypeRefNode(Token token) {
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