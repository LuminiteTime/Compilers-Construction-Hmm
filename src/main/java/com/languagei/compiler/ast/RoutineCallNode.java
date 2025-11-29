package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a routine call
 */
public class RoutineCallNode extends ASTNode {
    private final String name;
    private final List<ASTNode> arguments;

    public RoutineCallNode(Position position, String name) {
        super(position);
        this.name = name;
        this.arguments = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addArgument(ASTNode argument) {
        arguments.add(argument);
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        return new ArrayList<>(arguments);
    }
}

