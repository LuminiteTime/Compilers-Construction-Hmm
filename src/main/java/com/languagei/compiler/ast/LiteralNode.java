package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a literal value
 */
public class LiteralNode extends ASTNode {
    private final Object value;

    public LiteralNode(Position position, Object value) {
        super(position);
        this.value = value;
    }

    public Object getValue() {
        return value;
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
        return "Literal(" + value + ")";
    }
}

