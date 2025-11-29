package com.languagei.compiler.ast;

/**
 * Visitor interface for AST traversal (Visitor pattern)
 */
public interface ASTVisitor {
    void visit(ProgramNode node);
    void visit(VariableDeclarationNode node);
    void visit(TypeDeclarationNode node);
    void visit(RoutineDeclarationNode node);
    void visit(PrimitiveTypeNode node);
    void visit(ArrayTypeNode node);
    void visit(RecordTypeNode node);
    void visit(TypeReferenceNode node);
    void visit(BinaryExpressionNode node);
    void visit(UnaryExpressionNode node);
    void visit(LiteralNode node);
    void visit(IdentifierNode node);
    void visit(ArrayAccessNode node);
    void visit(RecordAccessNode node);
    void visit(RoutineCallNode node);
    void visit(AssignmentNode node);
    void visit(IfStatementNode node);
    void visit(WhileLoopNode node);
    void visit(ForLoopNode node);
    void visit(ReturnStatementNode node);
    void visit(PrintStatementNode node);
    void visit(BlockNode node);
}

