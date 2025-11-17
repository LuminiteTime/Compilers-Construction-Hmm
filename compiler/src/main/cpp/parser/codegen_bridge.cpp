/*
 * JNI Bridge for code generation
 * Bridges Java code generator with C++ AST
 */

#include <jni.h>
#include <iostream>
#include <sstream>
#include <string>
#include "ast.h"
#include "symbol.h"
#include "analyzer.h"

// Global AST pointer (set by parser)
extern ProgramNode* astRoot;
extern SymbolTable* symbolTable;

/**
 * Simple WAT emitter for code generation
 */
class WatEmitter {
private:
    std::stringstream output;
    int indentLevel = 0;
    static const std::string INDENT;

public:
    void startModule() {
        emit("(module");
        indentLevel++;
    }

    void endModule() {
        indentLevel--;
        emit(")");
    }

    void emitMemory(int pages) {
        emit("(memory " + std::to_string(pages) + ")");
    }

    void emitExport(const std::string& name, const std::string& kind, const std::string& item) {
        emit("(export \"" + name + "\" (" + kind + " $" + item + "))");
    }

    void emitGlobalHeapPtr() {
        emit("(global $heap_ptr (mut i32) (i32.const 0x1000))");
    }

    void emitComment(const std::string& text) {
        emit(";; " + text);
    }

    std::string toString() const {
        return output.str();
    }

private:
    void emit(const std::string& line) {
        for (int i = 0; i < indentLevel; i++) {
            output << INDENT;
        }
        output << line << "\n";
    }
};

const std::string WatEmitter::INDENT = "  ";

/**
 * Convert AST type to WASM type string
 */
static std::string getWasmType(TypeNode* type) {
    if (!type) return "i32";

    if (auto* prim = dynamic_cast<PrimitiveTypeNode*>(type)) {
        switch (prim->kind) {
            case TypeKind::INTEGER: return "i32";
            case TypeKind::REAL: return "f64";
            case TypeKind::BOOLEAN: return "i32";  // 0 = false, 1 = true
        }
    } else if (dynamic_cast<ArrayTypeNode*>(type)) {
        return "i32";  // pointer
    } else if (dynamic_cast<RecordTypeNode*>(type)) {
        return "i32";  // pointer
    }

    return "i32";
}

/**
 * Traverse AST and emit basic WASM structure
 */
static void generateWasmFromAST(ProgramNode* root, WatEmitter& emitter) {
    if (!root) {
        emitter.emitComment("Empty program");
        return;
    }

    emitter.emitComment("Program with " + std::to_string(root->declarations.size()) + " declarations");

    // Process declarations (simplified - full implementation would traverse all nodes)
    for (auto* decl : root->declarations) {
        if (auto* varDecl = dynamic_cast<VariableDeclarationNode*>(decl)) {
            emitter.emitComment("Variable: " + varDecl->name + " : " + 
                              (varDecl->type ? "type" : "inferred"));
        } else if (auto* typeDecl = dynamic_cast<TypeDeclarationNode*>(decl)) {
            emitter.emitComment("Type declaration: " + typeDecl->name);
        } else if (auto* routineDecl = dynamic_cast<RoutineDeclarationNode*>(decl)) {
            if (auto* header = dynamic_cast<RoutineHeaderNode*>(routineDecl->header)) {
                emitter.emitComment("Routine: " + header->name);
            }
        }
    }
}

/**
 * Java_compiler_codegen_CppASTBridge_generateWasmFromAST
 * 
 * Generates WASM code from C++ AST
 */
JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_generateWasmFromAST
  (JNIEnv *env, jobject obj, jlong astPointer) {
    try {
        if (!astRoot) {
            return env->NewStringUTF(
                "(module\n"
                "  (memory 1)\n"
                "  (export \"memory\" (memory 0))\n"
                "  (global $heap_ptr (mut i32) (i32.const 0x1000))\n"
                ")\n"
            );
        }

        WatEmitter emitter;
        emitter.startModule();
        emitter.emitMemory(1);
        emitter.emitExport("memory", "memory", "0");
        emitter.emitGlobalHeapPtr();
        
        // Generate from AST
        generateWasmFromAST(astRoot, emitter);
        
        emitter.endModule();

        std::string result = emitter.toString();
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& e) {
        std::string errorMsg = std::string("Code generation error: ") + e.what();
        env->ThrowNew(
            env->FindClass("compiler/codegen/CodeGenException"),
            errorMsg.c_str()
        );
        return env->NewStringUTF("");
    }
}

/**
 * Java_compiler_codegen_CppASTBridge_getASTAsJson
 * 
 * Returns AST structure as JSON for debugging
 */
JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_getASTAsJson
  (JNIEnv *env, jobject obj, jlong astPointer) {
    try {
        if (!astRoot) {
            return env->NewStringUTF("{\"declarations\": []}");
        }

        std::stringstream json;
        json << "{\"declarations\": [";
        
        bool first = true;
        for (auto* decl : astRoot->declarations) {
            if (!first) json << ", ";
            first = false;
            
            if (auto* varDecl = dynamic_cast<VariableDeclarationNode*>(decl)) {
                json << "{\"type\": \"variable\", \"name\": \"" << varDecl->name << "\"}";
            } else if (auto* typeDecl = dynamic_cast<TypeDeclarationNode*>(decl)) {
                json << "{\"type\": \"type\", \"name\": \"" << typeDecl->name << "\"}";
            } else if (auto* routineDecl = dynamic_cast<RoutineDeclarationNode*>(decl)) {
                if (auto* header = dynamic_cast<RoutineHeaderNode*>(routineDecl->header)) {
                    json << "{\"type\": \"routine\", \"name\": \"" << header->name << "\"}";
                }
            }
        }
        
        json << "]}";
        
        std::string result = json.str();
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& e) {
        return env->NewStringUTF("{\"error\": \"Failed to get AST as JSON\"}");
    }
}

