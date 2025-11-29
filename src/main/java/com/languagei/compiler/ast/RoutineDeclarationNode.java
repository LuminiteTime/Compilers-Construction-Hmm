package com.languagei.compiler.ast;

import com.languagei.compiler.lexer.Position;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a routine (function/procedure) declaration
 */
public class RoutineDeclarationNode extends ASTNode {
    private final String name;
    private final List<ParameterNode> parameters;
    private final ASTNode returnType; // Can be null for procedures
    private final BlockNode body; // Can be null for forward declarations
    private final boolean isForward;

    public RoutineDeclarationNode(Position position, String name, List<ParameterNode> parameters,
                                   ASTNode returnType, BlockNode body) {
        super(position);
        this.name = name;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.returnType = returnType;
        this.body = body;
        this.isForward = body == null;
    }

    public String getName() {
        return name;
    }

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public ASTNode getReturnType() {
        return returnType;
    }

    public BlockNode getBody() {
        return body;
    }

    public boolean isForward() {
        return isForward;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public List<ASTNode> getChildren() {
        List<ASTNode> children = new ArrayList<>(parameters);
        if (returnType != null) children.add(returnType);
        if (body != null) children.add(body);
        return children;
    }
}

