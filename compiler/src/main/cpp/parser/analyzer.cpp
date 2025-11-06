#include "analyzer.h"
#include <iostream>

extern SymbolTable* symbolTable;

static bool isIntegerType(TypeNode* type) {
    if (auto* prim = dynamic_cast<PrimitiveTypeNode*>(type)) {
        return prim->kind == TypeKind::INTEGER;
    }
    return false;
}

static bool asBoolLiteral(ExpressionNode* expr, bool& valueOut) {
    if (auto* b = dynamic_cast<BooleanLiteralNode*>(expr)) { valueOut = b->value; return true; }
    return false;
}

Analyzer::Result Analyzer::analyze(ProgramNode* root) {
    result = Result{};
    if (!root) {
        result.errors.push_back("Analyzer: null program root");
        return result;
    }
    runChecks(root);
    if (enableOpts && result.errors.empty()) {
        runOptimizations(root);
        // Post-optimization sanity checks (e.g., assignments to undeclared variables)
        // This helps catch cases where dead-branch elimination removed declarations
        // but later statements still reference those variables.
        // We run this even if symbol table scopes were permissive during parse.
        struct {
            void collectGlobals(ProgramNode* prog, std::unordered_set<std::string>& globals) {
                for (auto* d : prog->declarations) {
                    if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
                        globals.insert(vd->name);
                    }
                }
            }
            void checkExprForUndefined(ExpressionNode* expr, const std::unordered_set<std::string>& globals, Analyzer::Result& res) {
                if (!expr) return;
                if (auto* va = dynamic_cast<VariableAccessNode*>(expr)) {
                    if (globals.find(va->name) == globals.end()) {
                        res.errors.push_back(std::string("Undefined variable '") + va->name + "'");
                    }
                    return;
                }
                if (auto* bin = dynamic_cast<BinaryOpNode*>(expr)) {
                    checkExprForUndefined(bin->left, globals, res);
                    checkExprForUndefined(bin->right, globals, res);
                } else if (auto* un = dynamic_cast<UnaryOpNode*>(expr)) {
                    checkExprForUndefined(un->operand, globals, res);
                } else if (auto* arr = dynamic_cast<ArrayAccessNode*>(expr)) {
                    checkExprForUndefined(arr->array, globals, res);
                    checkExprForUndefined(arr->index, globals, res);
                } else if (auto* fld = dynamic_cast<FieldAccessNode*>(expr)) {
                    checkExprForUndefined(fld->record, globals, res);
                } else if (auto* call = dynamic_cast<RoutineCallNode*>(expr)) {
                    if (auto* al = dynamic_cast<ArgumentListNode*>(call->arguments)) {
                        for (auto* a : al->arguments) checkExprForUndefined(a, globals, res);
                    }
                }
            }
            void validateTopLevelReferences(ProgramNode* prog, const std::unordered_set<std::string>& globals, Analyzer::Result& res) {
                for (auto* s : prog->statements) {
                    if (auto* asg = dynamic_cast<AssignmentNode*>(s)) {
                        if (auto* va = dynamic_cast<VariableAccessNode*>(asg->target)) {
                            if (globals.find(va->name) == globals.end()) {
                                res.errors.push_back(std::string("Undefined variable '") + va->name + "'");
                            }
                        }
                        checkExprForUndefined(asg->value, globals, res);
                    } else if (auto* pr = dynamic_cast<PrintStatementNode*>(s)) {
                        if (auto* el = dynamic_cast<ExpressionListNode*>(pr->expressions)) {
                            for (auto* e : el->expressions) checkExprForUndefined(e, globals, res);
                        }
                    }
                    // Intentionally avoid descending into bodies to prevent false positives
                    // for properly scoped variables that still exist in non-constant branches.
                }
            }
        } post;
        std::unordered_set<std::string> globals;
        post.collectGlobals(root, globals);
        post.validateTopLevelReferences(root, globals, result);
    }
    return result;
}

void Analyzer::runChecks(ProgramNode* root) {
    for (auto* d : root->declarations) checkNode(d);
    for (auto* s : root->statements) checkStatement(s);
}

void Analyzer::checkNode(ASTNode* node) {
    if (!node) return;
    if (auto* vd = dynamic_cast<VariableDeclarationNode*>(node)) {
        if (vd->initializer) {
            checkExpression(vd->initializer);
            if (vd->type) {
                TypeNode* initT = inferType(vd->initializer);
                if (!typesCompatible(initT, vd->type)) {
                    result.errors.push_back("Type mismatch in variable initializer: " + vd->name);
                }
            }
        }
    } else if (auto* td = dynamic_cast<TypeDeclarationNode*>(node)) {
        if (auto* rec = dynamic_cast<RecordTypeNode*>(td->type)) {
            if (auto* body = dynamic_cast<RecordBodyNode*>(rec->body)) {
                std::unordered_set<std::string> seen;
                for (auto* f : body->fields) {
                    if (auto* v = dynamic_cast<VariableDeclarationNode*>(f)) {
                        if (!seen.insert(v->name).second) {
                            result.errors.push_back("Duplicate field '" + v->name + "' in type '" + td->name + "'");
                        }
                    }
                }
            }
        }
    } else if (auto* rd = dynamic_cast<RoutineDeclarationNode*>(node)) {
        if (auto* body = dynamic_cast<RoutineBodyNode*>(rd->body)) {
            if (auto* expr = dynamic_cast<ExpressionNode*>(body->body)) {
                auto* header = static_cast<RoutineHeaderNode*>(rd->header);
                if (header && header->returnType) {
                    auto* retT = inferType(expr);
                    if (!typesCompatible(retT, header->returnType)) {
                        result.errors.push_back("Routine '" + header->name + "' return type mismatch");
                    }
                }
            } else if (auto* b = dynamic_cast<BodyNode*>(body->body)) {
                for (auto* d : b->declarations) checkNode(d);
                for (auto* s : b->statements) checkStatement(s);
            }
        }
    }
}

void Analyzer::checkStatement(StatementNode* stmt) {
    if (!stmt) return;
    if (auto* asg = dynamic_cast<AssignmentNode*>(stmt)) {
        checkExpression(asg->target);
        checkExpression(asg->value);
        if (!checkAssignmentTypes(asg->target, asg->value)) {
            result.errors.push_back("Type mismatch in assignment");
        }
    } else if (auto* wh = dynamic_cast<WhileLoopNode*>(stmt)) {
        checkExpression(wh->condition);
        if (!isBooleanType(wh->condition)) {
            result.errors.push_back("While condition must be boolean");
        }
        if (auto* b = dynamic_cast<BodyNode*>(wh->body)) {
            for (auto* d : b->declarations) checkNode(d);
            for (auto* s : b->statements) checkStatement(s);
        }
    } else if (auto* fr = dynamic_cast<ForLoopNode*>(stmt)) {
        if (auto* r = dynamic_cast<RangeNode*>(fr->range)) {
            if (r->end) {
                auto* t1 = inferType(r->start);
                auto* t2 = inferType(r->end);
                if (!isIntegerType(t1) || !isIntegerType(t2)) {
                    result.errors.push_back("For range bounds must be integers");
                }
            } else {
                auto* t = inferType(r->start);
                if (!dynamic_cast<ArrayTypeNode*>(t)) {
                    result.errors.push_back("For-in expects array or numeric range");
                }
            }
        }
        if (auto* b = dynamic_cast<BodyNode*>(fr->body)) {
            for (auto* d : b->declarations) checkNode(d);
            for (auto* s : b->statements) checkStatement(s);
        }
    } else if (auto* iff = dynamic_cast<IfStatementNode*>(stmt)) {
        checkExpression(iff->condition);
        if (!isBooleanType(iff->condition)) {
            result.errors.push_back("If condition must be boolean");
        }
        if (auto* tb = dynamic_cast<BodyNode*>(iff->thenBody)) {
            for (auto* d : tb->declarations) checkNode(d);
            for (auto* s : tb->statements) checkStatement(s);
        }
        if (auto* eb = dynamic_cast<BodyNode*>(iff->elseBody)) {
            for (auto* d : eb->declarations) checkNode(d);
            for (auto* s : eb->statements) checkStatement(s);
        }
    } else if (auto* pr = dynamic_cast<PrintStatementNode*>(stmt)) {
        if (auto* el = dynamic_cast<ExpressionListNode*>(pr->expressions)) {
            for (auto* e : el->expressions) checkExpression(e);
        }
    } else if (auto* callStmt = dynamic_cast<RoutineCallStatementNode*>(stmt)) {
        checkRoutineCallTypes(callStmt->name, callStmt->arguments);
    }
}

void Analyzer::checkExpression(ExpressionNode* expr) {
    if (!expr) return;
    if (auto* bin = dynamic_cast<BinaryOpNode*>(expr)) {
        checkExpression(bin->left);
        checkExpression(bin->right);
    } else if (auto* un = dynamic_cast<UnaryOpNode*>(expr)) {
        checkExpression(un->operand);
    } else if (auto* arr = dynamic_cast<ArrayAccessNode*>(expr)) {
        checkExpression(arr->array);
        checkExpression(arr->index);
        checkArrayIndex(arr);
    } else if (auto* field = dynamic_cast<FieldAccessNode*>(expr)) {
        checkExpression(field->record);
        checkRecordFieldAccess(field);
    } else if (auto* call = dynamic_cast<RoutineCallNode*>(expr)) {
        checkRoutineCallTypes(call->name, call->arguments);
    }
}

void Analyzer::checkRecordFieldAccess(FieldAccessNode* field) {
    auto* recType = field->record ? field->record->type : nullptr;
    if (!recType) recType = inferType(field->record);
    auto* rt = dynamic_cast<RecordTypeNode*>(recType);
    if (!rt) {
        result.errors.push_back("Field access on non-record type");
        return;
    }
    auto* body = dynamic_cast<RecordBodyNode*>(rt->body);
    if (!body) return;
    bool found = false;
    for (auto* f : body->fields) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(f)) {
            if (vd->name == field->fieldName) { found = true; break; }
        }
    }
    if (!found) {
        result.errors.push_back("Unknown field '" + field->fieldName + "' in record");
    }
}

void Analyzer::checkArrayIndex(ArrayAccessNode* arrAcc) {
    auto* idxType = inferType(arrAcc->index);
    if (!isIntegerType(idxType)) {
        result.errors.push_back("Array index must be integer");
    }
    auto* arrType = arrAcc->array ? arrAcc->array->type : nullptr;
    if (!arrType) arrType = inferType(arrAcc->array);
    auto* at = dynamic_cast<ArrayTypeNode*>(arrType);
    if (at && at->size) {
        if (auto* lit = dynamic_cast<IntegerLiteralNode*>(arrAcc->index)) {
            if (auto* szLit = dynamic_cast<IntegerLiteralNode*>(at->size)) {
                int idx = lit->value;
                int sz = szLit->value;
                if (!(idx >= 1 && idx <= sz)) {
                    result.warnings.push_back("Array index " + std::to_string(idx) + " out of bounds [1.." + std::to_string(sz) + "] (static)");
                }
            }
        }
    }
}

void Analyzer::checkRoutineCallTypes(const std::string& name, ASTNode* arguments) {
    RoutineInfo* routine = symbolTable ? symbolTable->lookupRoutine(name) : nullptr;
    if (!routine) {
        result.errors.push_back("Undefined routine '" + name + "'");
        return;
    }
    std::vector<ExpressionNode*> args;
    if (auto* argList = dynamic_cast<ArgumentListNode*>(arguments)) {
        args = argList->arguments;
    } else if (auto* exprList = dynamic_cast<ExpressionListNode*>(arguments)) {
        args = exprList->expressions;
    }
    if (args.size() != routine->paramTypes.size()) {
        result.errors.push_back("Argument count mismatch in call to '" + name + "'");
        return;
    }
    for (size_t i = 0; i < args.size(); ++i) {
        auto* argT = inferType(args[i]);
        auto* paramT = routine->paramTypes[i];
        if (!typesCompatible(argT, paramT)) {
            result.errors.push_back("Argument type mismatch in call to '" + name + "' at position " + std::to_string(i+1));
        }
    }
}

void Analyzer::runOptimizations(ProgramNode* root) {
    for (auto* decl : root->declarations) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(decl)) {
            if (vd->initializer) {
                auto* folded = foldExpression(vd->initializer);
                if (folded != vd->initializer) { result.optimizationsApplied++; vd->initializer = folded; }
            }
        } else if (auto* rd = dynamic_cast<RoutineDeclarationNode*>(decl)) {
            if (auto* body = dynamic_cast<RoutineBodyNode*>(rd->body)) {
                if (auto* expr = dynamic_cast<ExpressionNode*>(body->body)) {
                    auto* folded = foldExpression(expr);
                    if (folded != expr) { result.optimizationsApplied++; body->body = folded; }
                } else if (auto* b = dynamic_cast<BodyNode*>(body->body)) {
                    simplifyInBody(b);
                }
            }
        }
    }
    simplifyInProgram(root);
    removeUnusedDeclarations(root);
}

ExpressionNode* Analyzer::foldExpression(ExpressionNode* expr) {
    if (!expr) return expr;
    if (auto* bin = dynamic_cast<BinaryOpNode*>(expr)) {
        bin->left = foldExpression(bin->left);
        bin->right = foldExpression(bin->right);
        auto* L_i = dynamic_cast<IntegerLiteralNode*>(bin->left);
        auto* R_i = dynamic_cast<IntegerLiteralNode*>(bin->right);
        auto* L_r = dynamic_cast<RealLiteralNode*>(bin->left);
        auto* R_r = dynamic_cast<RealLiteralNode*>(bin->right);
        auto* L_b = dynamic_cast<BooleanLiteralNode*>(bin->left);
        auto* R_b = dynamic_cast<BooleanLiteralNode*>(bin->right);
        if ((L_i && R_i) || (L_r && R_r) || (L_i && R_r) || (L_r && R_i)) {
            bool useReal = (L_r || R_r);
            double lv = L_r ? L_r->value : (L_i ? (double)L_i->value : 0.0);
            double rv = R_r ? R_r->value : (R_i ? (double)R_i->value : 0.0);
            switch (bin->op) {
                case OpKind::PLUS:   return useReal ? (ExpressionNode*)new RealLiteralNode(lv + rv) : (ExpressionNode*)new IntegerLiteralNode((int)(lv + rv));
                case OpKind::MINUS:  return useReal ? (ExpressionNode*)new RealLiteralNode(lv - rv) : (ExpressionNode*)new IntegerLiteralNode((int)(lv - rv));
                case OpKind::MUL:    return useReal ? (ExpressionNode*)new RealLiteralNode(lv * rv) : (ExpressionNode*)new IntegerLiteralNode((int)(lv * rv));
                case OpKind::DIV:    return (ExpressionNode*)new RealLiteralNode(lv / rv);
                case OpKind::MOD:    if (!useReal) return (ExpressionNode*)new IntegerLiteralNode((int)lv % (int)rv); else break;
                case OpKind::LT:     return (ExpressionNode*)new BooleanLiteralNode(lv < rv);
                case OpKind::LE:     return (ExpressionNode*)new BooleanLiteralNode(lv <= rv);
                case OpKind::GT:     return (ExpressionNode*)new BooleanLiteralNode(lv > rv);
                case OpKind::GE:     return (ExpressionNode*)new BooleanLiteralNode(lv >= rv);
                case OpKind::EQ:     return (ExpressionNode*)new BooleanLiteralNode(lv == rv);
                case OpKind::NE:     return (ExpressionNode*)new BooleanLiteralNode(lv != rv);
                default: break;
            }
        }
        if (L_b && R_b) {
            switch (bin->op) {
                case OpKind::AND: return (ExpressionNode*)new BooleanLiteralNode(L_b->value && R_b->value);
                case OpKind::OR:  return (ExpressionNode*)new BooleanLiteralNode(L_b->value || R_b->value);
                case OpKind::XOR: return (ExpressionNode*)new BooleanLiteralNode((bool)(L_b->value ^ R_b->value));
                default: break;
            }
        }
        return expr;
    }
    if (auto* un = dynamic_cast<UnaryOpNode*>(expr)) {
        un->operand = foldExpression(un->operand);
        if (auto* i = dynamic_cast<IntegerLiteralNode*>(un->operand)) {
            if (un->op == OpKind::UMINUS) return (ExpressionNode*)new IntegerLiteralNode(-i->value);
            if (un->op == OpKind::UPLUS)  return (ExpressionNode*)new IntegerLiteralNode(+i->value);
        }
        if (auto* r = dynamic_cast<RealLiteralNode*>(un->operand)) {
            if (un->op == OpKind::UMINUS) return (ExpressionNode*)new RealLiteralNode(-r->value);
            if (un->op == OpKind::UPLUS)  return (ExpressionNode*)new RealLiteralNode(+r->value);
        }
        if (auto* b = dynamic_cast<BooleanLiteralNode*>(un->operand)) {
            if (un->op == OpKind::NOT) return (ExpressionNode*)new BooleanLiteralNode(!b->value);
        }
        return expr;
    }
    if (auto* arr = dynamic_cast<ArrayAccessNode*>(expr)) {
        arr->array = foldExpression(arr->array);
        arr->index = foldExpression(arr->index);
        return expr;
    }
    if (auto* fld = dynamic_cast<FieldAccessNode*>(expr)) {
        fld->record = foldExpression(fld->record);
        return expr;
    }
    if (auto* call = dynamic_cast<RoutineCallNode*>(expr)) {
        if (auto* args = dynamic_cast<ArgumentListNode*>(call->arguments)) {
            for (auto*& a : args->arguments) a = foldExpression(a);
        }
        return expr;
    }
    return expr;
}

void Analyzer::simplifyInBody(BodyNode* body) {
    if (!body) return;
    for (auto* d : body->declarations) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
            if (vd->initializer) {
                auto* folded = foldExpression(vd->initializer);
                if (folded != vd->initializer) { result.optimizationsApplied++; vd->initializer = folded; }
            }
        }
    }
    std::vector<StatementNode*> newStmts;
    newStmts.reserve(body->statements.size());
    for (auto* s : body->statements) {
        if (auto* asg = dynamic_cast<AssignmentNode*>(s)) {
            asg->value = foldExpression(asg->value);
            newStmts.push_back(asg);
            continue;
        }
        if (auto* iff = dynamic_cast<IfStatementNode*>(s)) {
            iff->condition = foldExpression(iff->condition);
            bool val;
            if (asBoolLiteral(iff->condition, val)) {
                BodyNode* chosen = val ? dynamic_cast<BodyNode*>(iff->thenBody)
                                       : dynamic_cast<BodyNode*>(iff->elseBody);
                if (chosen) {
                    // First, simplify inside the chosen body to surface nested constant branches
                    simplifyInBody(chosen);
                    // Hoist declarations from the chosen branch into the current body scope
                    // Detect naming conflicts with existing declarations in this body
                    std::unordered_set<std::string> existing;
                    for (auto* d : body->declarations) {
                        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) existing.insert(vd->name);
                    }
                    for (auto* d : chosen->declarations) {
                        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
                            if (existing.find(vd->name) != existing.end()) {
                                result.errors.push_back("Duplicate variable declaration '" + vd->name + "' in same scope");
                            } else {
                                // Fold initializer of hoisted declaration
                                if (vd->initializer) {
                                    auto* folded = foldExpression(vd->initializer);
                                    if (folded != vd->initializer) { result.optimizationsApplied++; vd->initializer = folded; }
                                }
                                body->declarations.push_back(vd);
                                existing.insert(vd->name);
                            }
                        }
                    }
                    // Splice statements from the chosen branch
                    for (auto* inner : chosen->statements) newStmts.push_back(inner);
                }
                result.optimizationsApplied++;
                continue;
            } else {
                if (auto* tb = dynamic_cast<BodyNode*>(iff->thenBody)) simplifyInBody(tb);
                if (auto* eb = dynamic_cast<BodyNode*>(iff->elseBody)) simplifyInBody(eb);
                newStmts.push_back(iff);
                continue;
            }
        }
        if (auto* wh = dynamic_cast<WhileLoopNode*>(s)) {
            wh->condition = foldExpression(wh->condition);
            bool val;
            if (asBoolLiteral(wh->condition, val) && !val) { result.optimizationsApplied++; continue; }
            if (auto* b = dynamic_cast<BodyNode*>(wh->body)) simplifyInBody(b);
            newStmts.push_back(wh);
            continue;
        }
        newStmts.push_back(s);
    }
    body->statements.swap(newStmts);
}

void Analyzer::simplifyInProgram(ProgramNode* program) {
    std::vector<StatementNode*> newStmts;
    newStmts.reserve(program->statements.size());
    for (auto* s : program->statements) {
        if (auto* iff = dynamic_cast<IfStatementNode*>(s)) {
            iff->condition = foldExpression(iff->condition);
            bool val;
            if (asBoolLiteral(iff->condition, val)) {
                BodyNode* chosen = val ? dynamic_cast<BodyNode*>(iff->thenBody)
                                       : dynamic_cast<BodyNode*>(iff->elseBody);
                if (chosen) {
                    // Simplify inside chosen to hoist nested constant branches up to this body first
                    simplifyInBody(chosen);
                    // Hoist declarations into program scope with conflict detection
                    std::unordered_set<std::string> existing;
                    for (auto* d : program->declarations) {
                        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) existing.insert(vd->name);
                    }
                    for (auto* d : chosen->declarations) {
                        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
                            if (existing.find(vd->name) != existing.end()) {
                                result.errors.push_back("Duplicate variable declaration '" + vd->name + "' in same scope");
                            } else {
                                // Fold initializer of hoisted declaration
                                if (vd->initializer) {
                                    auto* folded = foldExpression(vd->initializer);
                                    if (folded != vd->initializer) { result.optimizationsApplied++; vd->initializer = folded; }
                                }
                                program->addDeclaration(vd);
                                existing.insert(vd->name);
                            }
                        }
                    }
                    // Splice statements from the chosen branch
                    for (auto* inner : chosen->statements) newStmts.push_back(inner);
                }
                result.optimizationsApplied++;
                continue;
            } else {
                if (auto* tb = dynamic_cast<BodyNode*>(iff->thenBody)) simplifyInBody(tb);
                if (auto* eb = dynamic_cast<BodyNode*>(iff->elseBody)) simplifyInBody(eb);
            }
        } else if (auto* wh = dynamic_cast<WhileLoopNode*>(s)) {
            wh->condition = foldExpression(wh->condition);
            bool val;
            if (asBoolLiteral(wh->condition, val) && !val) { result.optimizationsApplied++; continue; }
            if (auto* b = dynamic_cast<BodyNode*>(wh->body)) simplifyInBody(b);
        } else if (auto* asg = dynamic_cast<AssignmentNode*>(s)) {
            asg->value = foldExpression(asg->value);
        }
        newStmts.push_back(s);
    }
    program->statements.swap(newStmts);
}

void Analyzer::collectUsedVariables(ASTNode* node, std::unordered_set<std::string>& used) {
    if (!node) return;
    if (auto* expr = dynamic_cast<ExpressionNode*>(node)) {
        if (auto* va = dynamic_cast<VariableAccessNode*>(expr)) used.insert(va->name);
        else if (auto* bin = dynamic_cast<BinaryOpNode*>(expr)) { collectUsedVariables(bin->left, used); collectUsedVariables(bin->right, used);} 
        else if (auto* un = dynamic_cast<UnaryOpNode*>(expr)) { collectUsedVariables(un->operand, used);} 
        else if (auto* arr = dynamic_cast<ArrayAccessNode*>(expr)) { collectUsedVariables(arr->array, used); collectUsedVariables(arr->index, used);} 
        else if (auto* fld = dynamic_cast<FieldAccessNode*>(expr)) { collectUsedVariables(fld->record, used);} 
        else if (auto* call = dynamic_cast<RoutineCallNode*>(expr)) {
            if (auto* args = dynamic_cast<ArgumentListNode*>(call->arguments)) for (auto* a : args->arguments) collectUsedVariables(a, used);
        }
        return;
    }
    if (auto* stmt = dynamic_cast<StatementNode*>(node)) {
        if (auto* asg = dynamic_cast<AssignmentNode*>(stmt)) { collectUsedVariables(asg->target, used); collectUsedVariables(asg->value, used);} 
        else if (auto* wh = dynamic_cast<WhileLoopNode*>(stmt)) { collectUsedVariables(wh->condition, used); if (auto* b = dynamic_cast<BodyNode*>(wh->body)) { for (auto* d : b->declarations) collectUsedVariables(d, used); for (auto* s : b->statements) collectUsedVariables(s, used);} }
        else if (auto* fr = dynamic_cast<ForLoopNode*>(stmt)) { if (auto* r = dynamic_cast<RangeNode*>(fr->range)) { collectUsedVariables(r->start, used); if (r->end) collectUsedVariables(r->end, used);} if (auto* b = dynamic_cast<BodyNode*>(fr->body)) { for (auto* d : b->declarations) collectUsedVariables(d, used); for (auto* s : b->statements) collectUsedVariables(s, used);} }
        else if (auto* iff = dynamic_cast<IfStatementNode*>(stmt)) { collectUsedVariables(iff->condition, used); if (auto* tb = dynamic_cast<BodyNode*>(iff->thenBody)) { for (auto* d : tb->declarations) collectUsedVariables(d, used); for (auto* s : tb->statements) collectUsedVariables(s, used);} if (auto* eb = dynamic_cast<BodyNode*>(iff->elseBody)) { for (auto* d : eb->declarations) collectUsedVariables(d, used); for (auto* s : eb->statements) collectUsedVariables(s, used);} }
        else if (auto* pr = dynamic_cast<PrintStatementNode*>(stmt)) { if (auto* el = dynamic_cast<ExpressionListNode*>(pr->expressions)) for (auto* e : el->expressions) collectUsedVariables(e, used);} 
        else if (auto* callStmt = dynamic_cast<RoutineCallStatementNode*>(stmt)) { if (auto* al = dynamic_cast<ArgumentListNode*>(callStmt->arguments)) for (auto* a : al->arguments) collectUsedVariables(a, used);} 
        return;
    }
    if (auto* body = dynamic_cast<BodyNode*>(node)) { for (auto* d : body->declarations) collectUsedVariables(d, used); for (auto* s : body->statements) collectUsedVariables(s, used); return; }
    if (auto* prog = dynamic_cast<ProgramNode*>(node)) { for (auto* d : prog->declarations) collectUsedVariables(d, used); for (auto* s : prog->statements) collectUsedVariables(s, used); return; }
    if (auto* vd = dynamic_cast<VariableDeclarationNode*>(node)) { if (vd->initializer) collectUsedVariables(vd->initializer, used); return; }
    if (auto* rd = dynamic_cast<RoutineDeclarationNode*>(node)) { if (auto* body = dynamic_cast<RoutineBodyNode*>(rd->body)) { if (auto* expr = dynamic_cast<ExpressionNode*>(body->body)) collectUsedVariables(expr, used); if (auto* b = dynamic_cast<BodyNode*>(body->body)) collectUsedVariables(b, used);} return; }
}

void Analyzer::removeUnusedDeclarations(ProgramNode* program) {
    std::unordered_set<std::string> used;
    for (auto* s : program->statements) collectUsedVariables(s, used);
    for (auto* d : program->declarations) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) { if (vd->initializer) collectUsedVariables(vd->initializer, used); }
        else if (auto* rd = dynamic_cast<RoutineDeclarationNode*>(d)) { if (auto* body = dynamic_cast<RoutineBodyNode*>(rd->body)) { if (auto* expr = dynamic_cast<ExpressionNode*>(body->body)) collectUsedVariables(expr, used); if (auto* b = dynamic_cast<BodyNode*>(body->body)) collectUsedVariables(b, used);} }
    }
    std::vector<DeclarationNode*> newDecls;
    newDecls.reserve(program->declarations.size());
    for (auto* d : program->declarations) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
            bool isUsed = used.find(vd->name) != used.end();
            bool hasSideEffects = vd->initializer != nullptr;
            if (!isUsed && !hasSideEffects) { result.optimizationsApplied++; delete vd; continue; }
        }
        newDecls.push_back(d);
    }
    program->declarations.swap(newDecls);
    for (auto* d : program->declarations) {
        if (auto* rd = dynamic_cast<RoutineDeclarationNode*>(d)) {
            if (auto* body = dynamic_cast<RoutineBodyNode*>(rd->body)) {
                if (auto* b = dynamic_cast<BodyNode*>(body->body)) removeUnusedDeclarationsInBody(b, used);
            }
        }
    }
}

void Analyzer::removeUnusedDeclarationsInBody(BodyNode* body, const std::unordered_set<std::string>& used) {
    std::vector<ASTNode*> newDecls;
    newDecls.reserve(body->declarations.size());
    for (auto* d : body->declarations) {
        if (auto* vd = dynamic_cast<VariableDeclarationNode*>(d)) {
            bool isUsed = used.find(vd->name) != used.end();
            bool hasSideEffects = vd->initializer != nullptr;
            if (!isUsed && !hasSideEffects) { result.optimizationsApplied++; delete vd; continue; }
        }
        newDecls.push_back(d);
    }
    body->declarations.swap(newDecls);
}
