package compiler.parser;

import java.util.List;

import compiler.lexer.Token;

public class RecordTypeNode extends TypeNode {
    private final List<VariableDeclarationNode> fields;

    public RecordTypeNode(Token token, List<VariableDeclarationNode> fields) {
        super(token);
        this.fields = fields;
    }

    public List<VariableDeclarationNode> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("record\n");
        for (VariableDeclarationNode field : fields) {
            sb.append("  ").append(field).append("\n");
        }
        sb.append("end");
        return sb.toString();
    }
}
