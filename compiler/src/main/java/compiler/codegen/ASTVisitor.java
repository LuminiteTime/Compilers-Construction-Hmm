package compiler.codegen;

/**
 * Visitor interface for AST traversal
 * Enables clean separation between AST structure and code generation logic
 */
public interface ASTVisitor {
    // Program and declarations
    void visitProgram(Object programNode);
    void visitVariableDeclaration(Object node);
    void visitTypeDeclaration(Object node);
    void visitRoutineDeclaration(Object node);
    void visitRoutineHeader(Object node);
    void visitRoutineBody(Object node);

    // Statements
    void visitAssignment(Object node);
    void visitPrintStatement(Object node);
    void visitWhileLoop(Object node);
    void visitForLoop(Object node);
    void visitIfStatement(Object node);
    void visitReturnStatement(Object node);

    // Expressions
    void visitExpression(Object node);
    void visitBinaryOp(Object node);
    void visitUnaryOp(Object node);
    void visitIntegerLiteral(Object node);
    void visitRealLiteral(Object node);
    void visitBooleanLiteral(Object node);
    void visitStringLiteral(Object node);
    void visitVariableAccess(Object node);
    void visitArrayAccess(Object node);
    void visitFieldAccess(Object node);
    void visitRoutineCall(Object node);
}

