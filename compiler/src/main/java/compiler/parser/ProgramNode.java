package compiler.parser;

import java.util.List;

import compiler.lexer.Token;

public class ProgramNode extends AstNode {
    private final List<AstNode> declarations;

    public ProgramNode(Token token, List<AstNode> declarations) {
        super(token);
        this.declarations = declarations;
    }

    public List<AstNode> getDeclarations() {
        return declarations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (AstNode decl : declarations) {
            sb.append(decl).append("\n");
        }
        return sb.toString();
    }
}
