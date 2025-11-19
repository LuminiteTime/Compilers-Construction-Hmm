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

    ~SymbolTable() {
        // Clean up variable info
        for (auto& scope : variableScopes) {
            for (auto& pair : scope) {
                delete pair.second;
            }
        }
        // Clean up type info
        for (auto& pair : types) {
            delete pair.second;
        }
        // Clean up routine info
        for (auto& pair : routines) {
            delete pair.second;
        }
    }

    void enterScope() {
        variableScopes.emplace_back(); // new scope
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
        if (!headerNode) return;

        // Safe cast with type checking
        RoutineHeaderNode* header = dynamic_cast<RoutineHeaderNode*>(headerNode);
        if (!header) return; // Invalid header node

        std::vector<TypeNode*> paramTypes;

        // Safe parameter processing
        if (header->parameters) {
            ParameterListNode* params = dynamic_cast<ParameterListNode*>(header->parameters);
            if (params) {
                for (auto param : params->parameters) {
                    if (param) {
                        ParameterDeclarationNode* p = dynamic_cast<ParameterDeclarationNode*>(param);
                        if (p && p->type) {
                            paramTypes.push_back(p->type);
                        }
                    }
                }
            }
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

bool isRealType(TypeNode* type);
bool isBooleanType(TypeNode* type);
bool typesCompatible(TypeNode* t1, TypeNode* t2);

bool checkAssignmentTypes(ExpressionNode* target, ExpressionNode* value);
bool isBooleanType(ExpressionNode* expr);

bool checkArguments(RoutineInfo* routine, ASTNode* arguments);

void declareParameters(ASTNode* params);

#endif // SYMBOL_H
