#ifndef AST_H
#define AST_H

#include <vector>
#include <string>
#include <memory>

// Enums
enum class TypeKind { INTEGER, REAL, BOOLEAN };
enum class OpKind { PLUS, MINUS, MUL, DIV, MOD, LT, LE, GT, GE, EQ, NE, AND, OR, XOR, NOT, UPLUS, UMINUS };

// Base AST Node
class ASTNode {
public:
    virtual ~ASTNode() = default;
};

// Type Nodes
class TypeNode : public ASTNode {
public:
    virtual ~TypeNode() = default;
};

class PrimitiveTypeNode : public TypeNode {
public:
    TypeKind kind;
    PrimitiveTypeNode(TypeKind k) : kind(k) {}
};

class ArrayTypeNode : public TypeNode {
public:
    ASTNode* size; // ExpressionNode*
    TypeNode* elementType;
    ArrayTypeNode(ASTNode* s, TypeNode* et) : size(s), elementType(et) {}
};

class RecordTypeNode : public TypeNode {
public:
    ASTNode* body; // RecordBodyNode*
    RecordTypeNode(ASTNode* b) : body(b) {}
};

class RecordBodyNode : public ASTNode {
public:
    std::vector<ASTNode*> fields; // VariableDeclarationNode*
    void addField(ASTNode* field) { fields.push_back(field); }
};

// Expression Nodes
class ExpressionNode : public ASTNode {
public:
    TypeNode* type;
    ExpressionNode(TypeNode* t = nullptr) : type(t) {}
    virtual ~ExpressionNode() = default;
};

class IntegerLiteralNode : public ExpressionNode {
public:
    int value;
    IntegerLiteralNode(int v) : ExpressionNode(new PrimitiveTypeNode(TypeKind::INTEGER)), value(v) {}
};

class RealLiteralNode : public ExpressionNode {
public:
    double value;
    RealLiteralNode(double v) : ExpressionNode(new PrimitiveTypeNode(TypeKind::REAL)), value(v) {}
};

class BooleanLiteralNode : public ExpressionNode {
public:
    bool value;
    BooleanLiteralNode(bool v) : ExpressionNode(new PrimitiveTypeNode(TypeKind::BOOLEAN)), value(v) {}
};

class BinaryOpNode : public ExpressionNode {
public:
    OpKind op;
    ExpressionNode* left;
    ExpressionNode* right;
    BinaryOpNode(OpKind o, ExpressionNode* l, ExpressionNode* r) : op(o), left(l), right(r) {}
};

class UnaryOpNode : public ExpressionNode {
public:
    OpKind op;
    ExpressionNode* operand;
    UnaryOpNode(OpKind o, ExpressionNode* opnd) : op(o), operand(opnd) {}
};

class VariableAccessNode : public ExpressionNode {
public:
    std::string name;
    VariableAccessNode(const std::string& n, TypeNode* t) : ExpressionNode(t), name(n) {}
};

class FieldAccessNode : public ExpressionNode {
public:
    ExpressionNode* record;
    std::string fieldName;
    FieldAccessNode(ExpressionNode* r, const std::string& fn) : record(r), fieldName(fn) {}
};

class ArrayAccessNode : public ExpressionNode {
public:
    ExpressionNode* array;
    ExpressionNode* index;
    ArrayAccessNode(ExpressionNode* a, ExpressionNode* i) : array(a), index(i) {}
};

class RoutineCallNode : public ExpressionNode {
public:
    std::string name;
    ASTNode* arguments; // ArgumentListNode*
    RoutineCallNode(const std::string& n, ASTNode* args, TypeNode* rt) : ExpressionNode(rt), name(n), arguments(args) {}
};

class ExpressionListNode : public ASTNode {
public:
    std::vector<ExpressionNode*> expressions;
    void addExpression(ExpressionNode* expr) { expressions.push_back(expr); }
};

// Declaration Nodes
class DeclarationNode : public ASTNode {
public:
    virtual ~DeclarationNode() = default;
};

class VariableDeclarationNode : public DeclarationNode {
public:
    std::string name;
    TypeNode* type;
    ExpressionNode* initializer;
    VariableDeclarationNode(const std::string& n, TypeNode* t, ExpressionNode* init) : name(n), type(t), initializer(init) {}
};

class TypeDeclarationNode : public DeclarationNode {
public:
    std::string name;
    TypeNode* type;
    TypeDeclarationNode(const std::string& n, TypeNode* t) : name(n), type(t) {}
};

class RoutineDeclarationNode : public DeclarationNode {
public:
    ASTNode* header; // RoutineHeaderNode*
    ASTNode* body; // RoutineBodyNode*
    RoutineDeclarationNode(ASTNode* h, ASTNode* b) : header(h), body(b) {}
};

class RoutineHeaderNode : public ASTNode {
public:
    std::string name;
    ASTNode* parameters; // ParameterListNode*
    TypeNode* returnType;
    RoutineHeaderNode(const std::string& n, ASTNode* params, TypeNode* rt) : name(n), parameters(params), returnType(rt) {}
};

class ParameterListNode : public ASTNode {
public:
    std::vector<ASTNode*> parameters; // ParameterDeclarationNode*
    void addParameter(ASTNode* param) { parameters.push_back(param); }
};

class ParameterDeclarationNode : public ASTNode {
public:
    std::string name;
    TypeNode* type;
    ParameterDeclarationNode(const std::string& n, TypeNode* t) : name(n), type(t) {}
};

class RoutineBodyNode : public ASTNode {
public:
    ASTNode* body; // BodyNode* or ExpressionNode*
    RoutineBodyNode(ASTNode* b) : body(b) {}
};

// Statement Nodes
class StatementNode : public ASTNode {
public:
    virtual ~StatementNode() = default;
};

class AssignmentNode : public StatementNode {
public:
    ExpressionNode* target;
    ExpressionNode* value;
    AssignmentNode(ExpressionNode* t, ExpressionNode* v) : target(t), value(v) {}
};

class RoutineCallStatementNode : public StatementNode {
public:
    std::string name;
    ASTNode* arguments; // ArgumentListNode*
    RoutineCallStatementNode(const std::string& n, ASTNode* args) : name(n), arguments(args) {}
};

class WhileLoopNode : public StatementNode {
public:
    ExpressionNode* condition;
    ASTNode* body; // BodyNode*
    WhileLoopNode(ExpressionNode* cond, ASTNode* b) : condition(cond), body(b) {}
};

class ForLoopNode : public StatementNode {
public:
    std::string loopVar;
    ASTNode* range; // RangeNode*
    bool reverse;
    ASTNode* body; // BodyNode*
    ForLoopNode(const std::string& lv, ASTNode* r, bool rev, ASTNode* b) : loopVar(lv), range(r), reverse(rev), body(b) {}
};

class RangeNode : public ASTNode {
public:
    ExpressionNode* start;
    ExpressionNode* end;
    RangeNode(ExpressionNode* s, ExpressionNode* e) : start(s), end(e) {}
};

class IfStatementNode : public StatementNode {
public:
    ExpressionNode* condition;
    ASTNode* thenBody; // BodyNode*
    ASTNode* elseBody; // BodyNode*
    IfStatementNode(ExpressionNode* cond, ASTNode* tb, ASTNode* eb) : condition(cond), thenBody(tb), elseBody(eb) {}
};

class PrintStatementNode : public StatementNode {
public:
    ASTNode* expressions; // ExpressionListNode*
    PrintStatementNode(ASTNode* exprs) : expressions(exprs) {}
};

class ArgumentListNode : public ASTNode {
public:
    std::vector<ExpressionNode*> arguments;
    void addArgument(ExpressionNode* arg) { arguments.push_back(arg); }
};

class BodyNode : public ASTNode {
public:
    std::vector<ASTNode*> declarations; // DeclarationNode*
    std::vector<StatementNode*> statements;
    void addDeclaration(ASTNode* decl) { declarations.push_back(decl); }
    void addStatement(StatementNode* stmt) { statements.push_back(stmt); }
};

// Program Node
class ProgramNode : public ASTNode {
public:
    std::vector<DeclarationNode*> declarations;
    void addDeclaration(DeclarationNode* decl) { declarations.push_back(decl); }
};

#endif // AST_H