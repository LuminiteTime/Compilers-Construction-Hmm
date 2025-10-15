#ifndef SYMBOL_H
#define SYMBOL_H

#include <string>
#include <unordered_map>
#include <vector>
#include "ast.h"

// Forward declarations
class TypeNode;
class ExpressionNode;

// Info classes
class VariableInfo {
public:
    std::string name;
    TypeNode* type;
    VariableInfo(const std::string& n, TypeNode* t) : name(n), type(t) {}
};

class TypeInfo {
public:
    std::string name;
    TypeNode* type;
    TypeInfo(const std::string& n, TypeNode* t) : name(n), type(t) {}
};

class RoutineInfo {
public:
    std::string name;
    std::vector<TypeNode*> paramTypes;
    TypeNode* returnType;
    RoutineInfo(const std::string& n, const std::vector<TypeNode*>& pt, TypeNode* rt) : name(n), paramTypes(pt), returnType(rt) {}
};

// Symbol Table
class SymbolTable {
private:
    std::vector<std::unordered_map<std::string, VariableInfo*>> variableScopes;
    std::unordered_map<std::string, TypeInfo*> types;
    std::unordered_map<std::string, RoutineInfo*> routines;

public:
    SymbolTable() {
        variableScopes.emplace_back(); // global scope
    }

    void enterScope() {
        variableScopes.emplace_back();
    }

    void exitScope() {
        if (variableScopes.size() > 1) {
            variableScopes.pop_back();
        }
    }

    void declareVariable(const std::string& name, TypeNode* type) {
        VariableInfo* info = new VariableInfo(name, type);
        variableScopes.back()[name] = info;
    }

    VariableInfo* lookupVariable(const std::string& name) {
        for (auto it = variableScopes.rbegin(); it != variableScopes.rend(); ++it) {
            auto found = it->find(name);
            if (found != it->end()) {
                return found->second;
            }
        }
        return nullptr;
    }

    void declareType(const std::string& name, TypeNode* type) {
        TypeInfo* info = new TypeInfo(name, type);
        types[name] = info;
    }

    TypeNode* lookupType(const std::string& name) {
        auto it = types.find(name);
        if (it != types.end()) {
            return it->second->type;
        }
        return nullptr;
    }

    void declareRoutine(ASTNode* headerNode) {
        // Assuming RoutineHeaderNode
        RoutineHeaderNode* header = static_cast<RoutineHeaderNode*>(headerNode);
        std::vector<TypeNode*> paramTypes;
        ParameterListNode* params = static_cast<ParameterListNode*>(header->parameters);
        for (auto param : params->parameters) {
            ParameterDeclarationNode* p = static_cast<ParameterDeclarationNode*>(param);
            paramTypes.push_back(p->type);
        }
        RoutineInfo* info = new RoutineInfo(header->name, paramTypes, header->returnType);
        routines[header->name] = info;
    }

    RoutineInfo* lookupRoutine(const std::string& name) {
        auto it = routines.find(name);
        if (it != routines.end()) {
            return it->second;
        }
        return nullptr;
    }
};

// Utility functions
TypeNode* inferType(ExpressionNode* expr);

bool checkAssignmentTypes(ExpressionNode* target, ExpressionNode* value);

bool isBooleanType(ExpressionNode* expr);

bool checkArguments(RoutineInfo* routine, ASTNode* arguments);

#endif // SYMBOL_H