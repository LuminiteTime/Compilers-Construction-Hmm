package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a for loop
 */
public class ForLoopNode extends ASTNode {
    private final String variable;
    private final ASTNode rangeStart; // Can be null if iterating over array
    private final ASTNode rangeEnd; // Can be null if iterating over array
    private final ASTNode arrayExpr; // For iterating over array elements
    private final boolean reverse;
    private final BlockNode body;

    public ForLoopNode(Position position, String variable, ASTNode rangeStart, ASTNode rangeEnd,
                      ASTNode arrayExpr, boolean reverse, BlockNode body) {
        super(position);
        this.variable = variable;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.arrayExpr = arrayExpr;
        this.reverse = reverse;
        this.body = body;
    }

    public String getVariable() {
        return variable;
    }

    public ASTNode getRangeStart() {
        return rangeStart;
    }

    public ASTNode getRangeEnd() {
        return rangeEnd;
    }

    public ASTNode getArrayExpr() {
        return arrayExpr;
    }

    public boolean isReverse() {
        return reverse;
    }

    public BlockNode getBody() {
        return body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        if (rangeStart != null) children.add(rangeStart);
        if (rangeEnd != null) children.add(rangeEnd);
        if (arrayExpr != null) children.add(arrayExpr);
        children.add(body);
        return children;
    }
}

