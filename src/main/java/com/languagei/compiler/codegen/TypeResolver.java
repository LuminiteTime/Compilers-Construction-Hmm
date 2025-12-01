package com.languagei.compiler.codegen;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.semantic.Type;

/**
 * Resolves types for expressions during code generation.
 * Implements type inference for variables without explicit types.
 */
public class TypeResolver implements ASTVisitor {

    private Type currentType;
    private final VariableScopeManager scopeManager;

    public TypeResolver() {
        this.scopeManager = null;
    }

    public TypeResolver(VariableScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    public Type resolveType(ASTNode node) {
        currentType = null;
        node.accept(this);
        return currentType;
    }

    @Override
    public void visit(ProgramNode node) {
        // Not used for type resolution
    }

    @Override
    public void visit(VariableDeclarationNode node) {
        // Not used for type resolution
    }

    @Override
    public void visit(TypeDeclarationNode node) {
        // Not used for type resolution
    }

    @Override
    public void visit(RoutineDeclarationNode node) {
        // Not used for type resolution
    }

    @Override
    public void visit(PrimitiveTypeNode node) {
        switch (node.getType()) {
            case INTEGER -> currentType = Type.INTEGER;
            case REAL -> currentType = Type.REAL;
            case BOOLEAN -> currentType = Type.BOOLEAN;
        }
    }

    @Override
    public void visit(ArrayTypeNode node) {
        // Array type - for now return INTEGER as placeholder
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(RecordTypeNode node) {
        // Record type - for now return INTEGER as placeholder
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(TypeReferenceNode node) {
        // Type reference - for now return INTEGER as placeholder
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(BinaryExpressionNode node) {
        // For type inference, binary operations usually result in the common type
        node.getLeft().accept(this);
        Type leftType = currentType;

        node.getRight().accept(this);
        Type rightType = currentType;

        // Determine result type based on operation and operand types
        currentType = determineBinaryResultType(node.getOperator(), leftType, rightType);
    }

    @Override
    public void visit(UnaryExpressionNode node) {
        node.getOperand().accept(this);
        // Unary operations generally preserve type, except NOT which gives boolean
        if (node.getOperator() == UnaryExpressionNode.Operator.NOT) {
            currentType = Type.BOOLEAN;
        }
        // Other unary ops preserve type
    }

    @Override
    public void visit(LiteralNode node) {
        Object value = node.getValue();
        if (value instanceof Integer || value instanceof Long) {
            currentType = Type.INTEGER;
        } else if (value instanceof Double || value instanceof Float) {
            currentType = Type.REAL;
        } else if (value instanceof Boolean) {
            currentType = Type.BOOLEAN;
        } else {
            currentType = Type.INTEGER; // Default
        }
    }

    @Override
    public void visit(IdentifierNode node) {
        // Try to infer identifier type from the codegen variable scope, if available
        if (scopeManager != null) {
            VariableScopeManager.VariableInfo var = scopeManager.lookupVariable(node.getName());
            if (var != null) {
                if ("f64".equals(var.wasmType)) {
                    currentType = Type.REAL;
                    return;
                }
                // All other locals are represented as i32 (integers / booleans / pointers)
                currentType = Type.INTEGER;
                return;
            }
        }

        // Fallback: default to INTEGER when we have no better information
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(ArrayAccessNode node) {
        // Array access returns element type
        // For now, assume INTEGER elements
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(RecordAccessNode node) {
        // Record access returns field type
        // For now, assume INTEGER
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(RoutineCallNode node) {
        // Function call returns return type
        // For now, assume INTEGER
        currentType = Type.INTEGER;
    }

    @Override
    public void visit(AssignmentNode node) {
        // Assignment doesn't have a type
        currentType = null;
    }

    @Override
    public void visit(IfStatementNode node) {
        // Statement, no type
        currentType = null;
    }

    @Override
    public void visit(WhileLoopNode node) {
        // Statement, no type
        currentType = null;
    }

    @Override
    public void visit(ForLoopNode node) {
        // Statement, no type
        currentType = null;
    }

    @Override
    public void visit(ReturnStatementNode node) {
        // Statement, no type
        currentType = null;
    }

    @Override
    public void visit(PrintStatementNode node) {
        // Statement, no type
        currentType = null;
    }

    @Override
    public void visit(BlockNode node) {
        // Statement, no type
        currentType = null;
    }


    private Type determineBinaryResultType(BinaryExpressionNode.Operator operator, Type leftType, Type rightType) {
        // For arithmetic operations, result is the "higher" type
        if (operator == BinaryExpressionNode.Operator.PLUS ||
            operator == BinaryExpressionNode.Operator.MINUS ||
            operator == BinaryExpressionNode.Operator.MULTIPLY ||
            operator == BinaryExpressionNode.Operator.DIVIDE ||
            operator == BinaryExpressionNode.Operator.MODULO) {

            if (leftType == Type.REAL || rightType == Type.REAL) {
                return Type.REAL;
            } else {
                return Type.INTEGER;
            }
        }

        // For comparison operations, result is always boolean
        if (operator == BinaryExpressionNode.Operator.LT ||
            operator == BinaryExpressionNode.Operator.LE ||
            operator == BinaryExpressionNode.Operator.GT ||
            operator == BinaryExpressionNode.Operator.GE ||
            operator == BinaryExpressionNode.Operator.EQ ||
            operator == BinaryExpressionNode.Operator.NE) {

            return Type.BOOLEAN;
        }

        // For logical operations, result is boolean
        if (operator == BinaryExpressionNode.Operator.AND ||
            operator == BinaryExpressionNode.Operator.OR ||
            operator == BinaryExpressionNode.Operator.XOR) {

            return Type.BOOLEAN;
        }

        return Type.INTEGER; // Default
    }
}
