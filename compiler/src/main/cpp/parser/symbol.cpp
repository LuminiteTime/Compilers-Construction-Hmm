#include "symbol.h"
#include "ast.h"
#include <iostream>

// External symbol table
extern SymbolTable* symbolTable;

// Utility function implementations

TypeNode* inferType(ExpressionNode* expr) {
    if (!expr) return nullptr;

    // Literals
    if (auto* intLit = dynamic_cast<IntegerLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::INTEGER);
    } else if (auto* realLit = dynamic_cast<RealLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::REAL);
    } else if (auto* boolLit = dynamic_cast<BooleanLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::BOOLEAN);
    }

    // Binary operations
    else if (auto* binOp = dynamic_cast<BinaryOpNode*>(expr)) {
        TypeNode* leftType = inferType(binOp->left);
        TypeNode* rightType = inferType(binOp->right);

        // Arithmetic operations
        if (binOp->op == OpKind::PLUS || binOp->op == OpKind::MINUS ||
            binOp->op == OpKind::MUL || binOp->op == OpKind::DIV || binOp->op == OpKind::MOD) {
            if (isRealType(leftType) || isRealType(rightType)) {
                return new PrimitiveTypeNode(TypeKind::REAL);
            } else {
                return new PrimitiveTypeNode(TypeKind::INTEGER);
            }
        }

        // Comparison operations
        else if (binOp->op == OpKind::LT || binOp->op == OpKind::LE ||
                 binOp->op == OpKind::GT || binOp->op == OpKind::GE ||
                 binOp->op == OpKind::EQ || binOp->op == OpKind::NE) {
            return new PrimitiveTypeNode(TypeKind::BOOLEAN);
        }

        // Logical operations
        else if (binOp->op == OpKind::AND || binOp->op == OpKind::OR || binOp->op == OpKind::XOR) {
            return new PrimitiveTypeNode(TypeKind::BOOLEAN);
        }
    }

    // Unary operations
    else if (auto* unaryOp = dynamic_cast<UnaryOpNode*>(expr)) {
        if (unaryOp->op == OpKind::NOT) {
            return new PrimitiveTypeNode(TypeKind::BOOLEAN);
        } else {
            return inferType(unaryOp->operand);
        }
    }

    // Variables and access
    else if (expr->type) {
        return expr->type;
    }

    // Default fallback
    return new PrimitiveTypeNode(TypeKind::INTEGER);
}

bool isRealType(TypeNode* type) {
    if (auto* prim = dynamic_cast<PrimitiveTypeNode*>(type)) {
        return prim->kind == TypeKind::REAL;
    }
    return false;
}

bool isBooleanType(TypeNode* type) {
    if (auto* prim = dynamic_cast<PrimitiveTypeNode*>(type)) {
        return prim->kind == TypeKind::BOOLEAN;
    }
    return false;
}

bool typesCompatible(TypeNode* t1, TypeNode* t2) {
    if (!t1 || !t2) return false;

    // Same primitive types
    auto* p1 = dynamic_cast<PrimitiveTypeNode*>(t1);
    auto* p2 = dynamic_cast<PrimitiveTypeNode*>(t2);
    if (p1 && p2) {
        return p1->kind == p2->kind;
    }

    // Integer can be assigned to real (implicit conversion)
    if (p1 && p2 && p1->kind == TypeKind::INTEGER && p2->kind == TypeKind::REAL) {
        return true;
    }

    // For now, assume other types are compatible
    return true;
}

bool checkAssignmentTypes(ExpressionNode* target, ExpressionNode* value) {
    if (!target || !value) return false;

    // Special case for field access - allow for now
    if (dynamic_cast<FieldAccessNode*>(target)) {
        return true;
    }

    // Infer types if not set
    if (!target->type) target->type = inferType(target);
    if (!value->type) value->type = inferType(value);

    return typesCompatible(target->type, value->type);
}

bool isBooleanType(ExpressionNode* expr) {
    if (!expr) return false;
    if (!expr->type) expr->type = inferType(expr);
    return isBooleanType(expr->type);
}

bool checkArguments(RoutineInfo* routine, ASTNode* arguments) {
    if (!arguments) return routine->paramTypes.empty();

    // Try ArgumentListNode first
    ArgumentListNode* argList = dynamic_cast<ArgumentListNode*>(arguments);
    if (argList) {
        return argList->arguments.size() == routine->paramTypes.size();
    }

    // Try ExpressionListNode
    ExpressionListNode* exprList = dynamic_cast<ExpressionListNode*>(arguments);
    if (exprList) {
        return exprList->expressions.size() == routine->paramTypes.size();
    }

    return false;
}

void declareParameters(ASTNode* params) {
    if (!params) return;
    ParameterListNode* paramList = static_cast<ParameterListNode*>(params);
    for (auto param : paramList->parameters) {
        ParameterDeclarationNode* p = static_cast<ParameterDeclarationNode*>(param);
        symbolTable->declareVariable(p->name, p->type);
    }
}
