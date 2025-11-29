package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a print statement
 */
public class PrintStatementNode extends ASTNode {
    private final List<ASTNode> expressions;

    public PrintStatementNode(Position position) {
        super(position);
        this.expressions = new ArrayList<>();
    }

    public void addExpression(ASTNode expression) {
        expressions.add(expression);
    }

    public List<ASTNode> getExpressions() {
        return expressions;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(expressions);
    }
}

