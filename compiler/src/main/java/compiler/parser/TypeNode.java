package compiler.parser;

import compiler.lexer.Token;

public abstract class TypeNode extends AstNode {
    protected TypeNode(Token token) {
        super(token);
    }
}