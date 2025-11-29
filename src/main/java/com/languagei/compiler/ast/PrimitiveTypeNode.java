package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a primitive type (integer, real, boolean)
 */
public class PrimitiveTypeNode extends ASTNode {
    public enum PrimitiveType {
        INTEGER, REAL, BOOLEAN
    }

    private final PrimitiveType type;

    public PrimitiveTypeNode(Position position, PrimitiveType type) {
        super(position);
        this.type = type;
    }

    public PrimitiveType getType() {
        return type;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "PrimitiveType(" + type + ")";
    }
}

