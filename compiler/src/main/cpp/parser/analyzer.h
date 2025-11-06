#ifndef ANALYZER_H
#define ANALYZER_H

#include <vector>
#include <string>
#include <unordered_set>
#include "ast.h"
#include "symbol.h"

extern SymbolTable* symbolTable;

class Analyzer {
public:
    struct Result {
        std::vector<std::string> errors;
        std::vector<std::string> warnings;
        size_t optimizationsApplied = 0;
        bool success() const { return errors.empty(); }
    };

    explicit Analyzer(bool enableOptimizations = true)
        : enableOpts(enableOptimizations) {}

    Result analyze(ProgramNode* root);

private:
    bool enableOpts;
    Result result;

    // Checks (no AST modification)
    void runChecks(ProgramNode* root);
    void checkNode(ASTNode* node);
    void checkExpression(ExpressionNode* expr);
    void checkStatement(StatementNode* stmt);
    void checkRecordFieldAccess(FieldAccessNode* field);
    void checkArrayIndex(ArrayAccessNode* arrAcc);
    void checkRoutineCallTypes(const std::string& name, ASTNode* arguments);

    // Optimizations (AST modification)
    void runOptimizations(ProgramNode* root);
    ExpressionNode* foldExpression(ExpressionNode* expr);
    void simplifyInBody(BodyNode* body);
    void simplifyInProgram(ProgramNode* program);
    void removeUnusedDeclarations(ProgramNode* program);
    void removeUnusedDeclarationsInBody(BodyNode* body, const std::unordered_set<std::string>& used);
    void collectUsedVariables(ASTNode* node, std::unordered_set<std::string>& used);
};

#endif // ANALYZER_H
