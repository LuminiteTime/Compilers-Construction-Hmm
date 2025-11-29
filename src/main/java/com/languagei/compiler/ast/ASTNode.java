package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.List;

/**
 * Abstract base class for all AST nodes
 */
public abstract class ASTNode {
    protected Position position;

    public ASTNode(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }

    public abstract void accept(ASTVisitor visitor);

    public abstract List<ASTNode> getChildren();

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}

