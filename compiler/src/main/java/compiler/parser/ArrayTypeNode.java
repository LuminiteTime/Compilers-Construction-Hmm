package compiler.parser;

import compiler.lexer.Token;

public class ArrayTypeNode extends TypeNode {
    private final ExpressionNode size; // can be null for []
    private final TypeNode elementType;

    public ArrayTypeNode(Token token, ExpressionNode size, TypeNode elementType) {
        super(token);
        this.size = size;
        this.elementType = elementType;
    }

    public ExpressionNode getSize() {
        return size;
    }

    public TypeNode getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        String sizeStr = size != null ? "[" + size + "]" : "[]";
        return "array" + sizeStr + " " + elementType;
    }
}