package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a routine parameter
 */
public class ParameterNode extends ASTNode {
    private final String name;
    private final ASTNode type;

    public ParameterNode(Position position, String name, ASTNode type) {
        super(position);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ASTNode getType() {
        return type;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        // Parameters don't have their own visitor method
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(type);
        return children;
    }
}

