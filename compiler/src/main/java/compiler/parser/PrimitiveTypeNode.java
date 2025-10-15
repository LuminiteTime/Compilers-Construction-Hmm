package compiler.parser;

import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class PrimitiveTypeNode extends TypeNode {
    private final TypeKind kind;

    public PrimitiveTypeNode(Token token) {
        super(token);
        switch (token.getType()) {
            case INTEGER -> this.kind = TypeKind.INTEGER;
            case REAL -> this.kind = TypeKind.REAL;
            case BOOLEAN -> this.kind = TypeKind.BOOLEAN;
            default -> throw new IllegalArgumentException("Invalid primitive type: " + token.getType());
        }
    }

    public TypeKind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase();
    }
}