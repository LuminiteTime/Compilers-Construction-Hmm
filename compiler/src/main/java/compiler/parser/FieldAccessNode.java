package compiler.parser;

import compiler.lexer.Token;

public class FieldAccessNode extends AstNode implements ExpressionNode {
    private final ExpressionNode record;
    private final String fieldName;

    public FieldAccessNode(Token token, ExpressionNode record, String fieldName) {
        super(token);
        this.record = record;
        this.fieldName = fieldName;
    }

    public ExpressionNode getRecord() {
        return record;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return record + "." + fieldName;
    }
}