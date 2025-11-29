package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an if-then-else statement
 */
public class IfStatementNode extends ASTNode {
    private final ASTNode condition;
    private final BlockNode thenBlock;
    private final BlockNode elseBlock; // Can be null

    public IfStatementNode(Position position, ASTNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        super(position);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return thenBlock;
    }

    public BlockNode getElseBlock() {
        return elseBlock;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(condition);
        children.add(thenBlock);
        if (elseBlock != null) children.add(elseBlock);
        return children;
    }
}

