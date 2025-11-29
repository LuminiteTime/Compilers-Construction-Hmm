package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a while loop
 */
public class WhileLoopNode extends ASTNode {
    private final ASTNode condition;
    private final BlockNode body;

    public WhileLoopNode(Position position, ASTNode condition, BlockNode body) {
        super(position);
        this.condition = condition;
        this.body = body;
    }

    public ASTNode getCondition() {
        return condition;
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
        children.add(condition);
        children.add(body);
        return children;
    }
}

