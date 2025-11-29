package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Root node of the AST representing the entire program
 */
public class ProgramNode extends ASTNode {
    private final List<ASTNode> declarations;
    private final List<ASTNode> statements;

    public ProgramNode(Position position) {
        super(position);
        this.declarations = new ArrayList<>();
        this.statements = new ArrayList<>();
    }

    public void addDeclaration(ASTNode declaration) {
        declarations.add(declaration);
    }

    public void addStatement(ASTNode statement) {
        statements.add(statement);
    }

    public List<ASTNode> getDeclarations() {
        return declarations;
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>(declarations);
        children.addAll(statements);
        return children;
    }
}

