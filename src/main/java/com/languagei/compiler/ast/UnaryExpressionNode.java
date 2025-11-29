package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a unary expression
 */
public class UnaryExpressionNode extends ASTNode {
    public enum Operator {
        PLUS, MINUS, NOT
    }

    private final Operator operator;
    private final ASTNode operand;

    public UnaryExpressionNode(Position position, Operator operator, ASTNode operand) {
        super(position);
        this.operator = operator;
        this.operand = operand;
    }

    public Operator getOperator() {
        return operator;
    }

    public ASTNode getOperand() {
        return operand;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(operand);
        return children;
    }
}

