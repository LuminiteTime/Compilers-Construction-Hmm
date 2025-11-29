package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reference to a type by name
 */
public class TypeReferenceNode extends ASTNode {
    private final String name;

    public TypeReferenceNode(Position position, String name) {
        super(position);
        this.name = name;
    }

    public String getName() {
        return name;
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
        return "TypeRef(" + name + ")";
    }
}

