package compiler.parser;

import compiler.lexer.Token;

public class TypeDeclarationNode extends AstNode {
    private final String name;
    private final TypeNode type;

    public TypeDeclarationNode(Token token, String name, TypeNode type) {
        super(token);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TypeNode getType() {
        return type;
    }

    @Override
    public String toString() {
        return "type " + name + " is " + type;
    }
}