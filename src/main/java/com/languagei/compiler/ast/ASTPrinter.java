package com.languagei.compiler.ast;

import java.util.List;

/**
 * Utility for pretty-printing AST trees.
 */
public class ASTPrinter {

    public static void print(ASTNode node) {
        print(node, 0);
    }

    private static void print(ASTNode node, int indent) {
        if (node == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) sb.append("  ");
        sb.append(format(node));
        System.out.println(sb.toString());

        List<ASTNode> children = node.getChildren();
        if (children == null) return;
        for (ASTNode child : children) {
            print(child, indent + 1);
        }
    }

    private static String format(ASTNode node) {
        if (node instanceof LiteralNode) {
            return node.toString();
        }
        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode v = (VariableDeclarationNode) node;
            return "VarDecl(" + v.getName() + ")";
        }
        if (node instanceof IdentifierNode) {
            IdentifierNode id = (IdentifierNode) node;
            return "Identifier(" + id.getName() + ")";
        }
        if (node instanceof BinaryExpressionNode) {
            BinaryExpressionNode b = (BinaryExpressionNode) node;
            return "Binary(" + b.getOperator() + ")";
        }
        if (node instanceof UnaryExpressionNode) {
            UnaryExpressionNode u = (UnaryExpressionNode) node;
            return "Unary(" + u.getOperator() + ")";
        }
        if (node instanceof IfStatementNode) {
            return "If";
        }
        if (node instanceof WhileLoopNode) {
            return "While";
        }
        if (node instanceof AssignmentNode) {
            return "Assignment";
        }
        if (node instanceof PrintStatementNode) {
            return "Print";
        }
        if (node instanceof RoutineDeclarationNode) {
            RoutineDeclarationNode r = (RoutineDeclarationNode) node;
            return "Routine(" + r.getName() + ")";
        }
        if (node instanceof ProgramNode) {
            return "Program";
        }
        return node.getClass().getSimpleName();
    }
}
