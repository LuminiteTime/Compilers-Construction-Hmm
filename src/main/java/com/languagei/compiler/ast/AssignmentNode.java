package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an assignment statement
 */
public class AssignmentNode extends ASTNode {
    private final ASTNode target;
    private final ASTNode value;

    public AssignmentNode(Position position, ASTNode target, ASTNode value) {
        super(position);
        this.target = target;
        this.value = value;
    }

    public ASTNode getTarget() {
        return target;
    }

    public ASTNode getValue() {
        return value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(target);
        children.add(value);
        return children;
    }
}

