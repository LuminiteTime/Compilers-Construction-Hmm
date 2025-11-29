package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a variable declaration
 */
public class VariableDeclarationNode extends ASTNode {
    private final String name;
    private final ASTNode type; // Can be null for type inference
    private final ASTNode initializer; // Can be null

    public VariableDeclarationNode(Position position, String name, ASTNode type, ASTNode initializer) {
        super(position);
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    public String getName() {
        return name;
    }

    public ASTNode getType() {
        return type;
    }

    public ASTNode getInitializer() {
        return initializer;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        if (type != null) children.add(type);
        if (initializer != null) children.add(initializer);
        return children;
    }
}

