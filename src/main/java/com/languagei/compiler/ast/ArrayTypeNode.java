package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an array type
 */
public class ArrayTypeNode extends ASTNode {
    private final ASTNode sizeExpression; // Can be null for sizeless arrays
    private final ASTNode elementType;

    public ArrayTypeNode(Position position, ASTNode sizeExpression, ASTNode elementType) {
        super(position);
        this.sizeExpression = sizeExpression;
        this.elementType = elementType;
    }

    public ASTNode getSizeExpression() {
        return sizeExpression;
    }

    public ASTNode getElementType() {
        return elementType;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        if (sizeExpression != null) children.add(sizeExpression);
        children.add(elementType);
        return children;
    }
}

