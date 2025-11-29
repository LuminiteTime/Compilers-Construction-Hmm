package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a binary expression
 */
public class BinaryExpressionNode extends ASTNode {
    public enum Operator {
        PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
        AND, OR, XOR,
        LT, LE, GT, GE, EQ, NE
    }

    private final ASTNode left;
    private final Operator operator;
    private final ASTNode right;

    public BinaryExpressionNode(Position position, ASTNode left, Operator operator, ASTNode right) {
        super(position);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public ASTNode getLeft() {
        return left;
    }

    public Operator getOperator() {
        return operator;
    }

    public ASTNode getRight() {
        return right;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(left);
        children.add(right);
        return children;
    }
}

