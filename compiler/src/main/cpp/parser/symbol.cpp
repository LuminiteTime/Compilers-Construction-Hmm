#include "symbol.h"
#include "ast.h"
#include <iostream>

// Utility function implementations

TypeNode* inferType(ExpressionNode* expr) {
    // Simple type inference based on node type
    if (dynamic_cast<IntegerLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::INTEGER);
    } else if (dynamic_cast<RealLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::REAL);
    } else if (dynamic_cast<BooleanLiteralNode*>(expr)) {
        return new PrimitiveTypeNode(TypeKind::BOOLEAN);
    } else {
        // For more complex expressions, assume integer for now
        return new PrimitiveTypeNode(TypeKind::INTEGER);
    }
}

bool checkAssignmentTypes(ExpressionNode* target, ExpressionNode* value) {
    // Simple type checking: assume compatible if same primitive type
    PrimitiveTypeNode* targetType = dynamic_cast<PrimitiveTypeNode*>(target->type);
    PrimitiveTypeNode* valueType = dynamic_cast<PrimitiveTypeNode*>(value->type);
    if (targetType && valueType) {
        return targetType->kind == valueType->kind;
    }
    return true; // For now, assume compatible
}

bool isBooleanType(ExpressionNode* expr) {
    PrimitiveTypeNode* type = dynamic_cast<PrimitiveTypeNode*>(expr->type);
    return type && type->kind == TypeKind::BOOLEAN;
}

bool checkArguments(RoutineInfo* routine, ASTNode* arguments) {
    ArgumentListNode* args = dynamic_cast<ArgumentListNode*>(arguments);
    if (!args) return false;
    if (args->arguments.size() != routine->paramTypes.size()) return false;
    for (size_t i = 0; i < args->arguments.size(); ++i) {
        // Simple check: assume compatible
        // In real implementation, check types
    }
    return true;
}