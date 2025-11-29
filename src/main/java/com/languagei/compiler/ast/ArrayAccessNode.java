package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents array element access
 */
public class ArrayAccessNode extends ASTNode {
    private final ASTNode array;
    private final ASTNode index;

    public ArrayAccessNode(Position position, ASTNode array, ASTNode index) {
        super(position);
        this.array = array;
        this.index = index;
    }

    public ASTNode getArray() {
        return array;
    }

    public ASTNode getIndex() {
        return index;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(array);
        children.add(index);
        return children;
    }
}

