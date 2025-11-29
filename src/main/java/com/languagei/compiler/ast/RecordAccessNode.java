package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents record field access or array size access
 */
public class RecordAccessNode extends ASTNode {
    private final ASTNode object;
    private final String fieldName;

    public RecordAccessNode(Position position, ASTNode object, String fieldName) {
        super(position);
        this.object = object;
        this.fieldName = fieldName;
    }

    public ASTNode getObject() {
        return object;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>();
        children.add(object);
        return children;
    }
}

