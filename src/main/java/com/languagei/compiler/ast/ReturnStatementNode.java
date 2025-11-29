package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a return statement
 */
public class ReturnStatementNode extends ASTNode {
    private final ASTNode value; // Can be null for void returns

    public ReturnStatementNode(Position position, ASTNode value) {
        super(position);
        this.value = value;
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
        if (value != null) children.add(value);
        return children;
    }
}

