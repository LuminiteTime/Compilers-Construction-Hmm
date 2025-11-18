/*
 * JNI Bridge for AST access
 * Provides access to C++ parsed AST for Java code generator
 */

#include <jni.h>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <unordered_map>
#include <unordered_set>
#include "ast.h"
#include "symbol.h"
#include "analyzer.h"

// Global AST pointer (set by parser)
extern ProgramNode* astRoot;
extern SymbolTable* symbolTable;

/**
 * Convert AST node to JSON representation
 */
static std::string astNodeToJson(ASTNode* node, int depth = 0) {
    if (!node) return "null";
    
    std::stringstream json;
    std::string indent(depth * 2, ' ');
    
    if (auto* program = dynamic_cast<ProgramNode*>(node)) {
        json << "{\n";
        json << indent << "  \"type\": \"program\",\n";
        json << indent << "  \"declarations\": [\n";
        
        for (size_t i = 0; i < program->declarations.size(); ++i) {
            json << indent << "    " << astNodeToJson(program->declarations[i], depth + 2);
            if (i < program->declarations.size() - 1) json << ",";
            json << "\n";
        }
        
        json << indent << "  ],\n";
        json << indent << "  \"statements\": [\n";
        
        for (size_t i = 0; i < program->statements.size(); ++i) {
            json << indent << "    " << astNodeToJson(program->statements[i], depth + 2);
            if (i < program->statements.size() - 1) json << ",";
            json << "\n";
        }
        
        json << indent << "  ]\n";
        json << indent << "}";
        
    } else if (auto* varDecl = dynamic_cast<VariableDeclarationNode*>(node)) {
        json << "{\"type\": \"variable\", \"name\": \"" << varDecl->name << "\"";
        json << ", \"varType\": \"integer\"";  // Simplified
        json << "}";

    } else if (auto* assign = dynamic_cast<AssignmentNode*>(node)) {
        json << "{\"type\": \"assignment\", \"target\": ";
        json << astNodeToJson(assign->target, depth + 1);
        json << ", \"value\": ";
        json << astNodeToJson(assign->value, depth + 1);
        json << "}";

    } else if (auto* print = dynamic_cast<PrintStatementNode*>(node)) {
        json << "{\"type\": \"print\", \"expressions\": ";
        json << astNodeToJson(print->expressions, depth + 1);
        json << "}";

    } else if (auto* exprList = dynamic_cast<ExpressionListNode*>(node)) {
        json << "[";
        for (size_t i = 0; i < exprList->expressions.size(); ++i) {
            json << astNodeToJson(exprList->expressions[i], depth + 1);
            if (i < exprList->expressions.size() - 1) json << ", ";
        }
        json << "]";

    } else if (auto* varAccess = dynamic_cast<VariableAccessNode*>(node)) {
        json << "{\"type\": \"variable_access\", \"name\": \"" << varAccess->name << "\"}";

    } else if (auto* intLit = dynamic_cast<IntegerLiteralNode*>(node)) {
        json << "{\"type\": \"integer_literal\", \"value\": " << intLit->value << "}";

    } else if (auto* realLit = dynamic_cast<RealLiteralNode*>(node)) {
        json << "{\"type\": \"real_literal\", \"value\": " << realLit->value << "}";

    } else {
        json << "{\"type\": \"unknown\"}";
    }
    
    return json.str();
}

/**
 * Java_compiler_codegen_CppASTBridge_getASTPointer
 *
 * Returns pointer to the parsed AST
 */
extern "C" JNIEXPORT jlong JNICALL Java_compiler_codegen_CppASTBridge_getASTPointer
  (JNIEnv *env, jobject obj) {
    std::cout << "DEBUG: getASTPointer called, astRoot=" << (void*)astRoot << std::endl;
    return reinterpret_cast<jlong>(astRoot);
}

/**
 * Java_compiler_codegen_CppASTBridge_getASTAsJson
 *
 * Returns AST structure as JSON for Java code generator
 */
extern "C" JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_getASTAsJson
  (JNIEnv *env, jobject obj, jlong astPointer) {
    try {
        if (!astRoot) {
            return env->NewStringUTF("{\"type\": \"program\", \"declarations\": [], \"statements\": []}");
        }

        std::string json = astNodeToJson(astRoot);
        return env->NewStringUTF(json.c_str());

    } catch (const std::exception& e) {
        std::cerr << "Error generating AST JSON: " << e.what() << std::endl;
        return env->NewStringUTF("{\"type\": \"program\", \"declarations\": [], \"statements\": []}");
    }
}
