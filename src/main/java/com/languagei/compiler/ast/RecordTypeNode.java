package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a record type
 */
public class RecordTypeNode extends ASTNode {
    private final List<VariableDeclarationNode> fields;

    public RecordTypeNode(Position position) {
        super(position);
        this.fields = new ArrayList<>();
    }

    public void addField(VariableDeclarationNode field) {
        fields.add(field);
    }

    public List<VariableDeclarationNode> getFields() {
        return fields;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(fields);
    }
}

