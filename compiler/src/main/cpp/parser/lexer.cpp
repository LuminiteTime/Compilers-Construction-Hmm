#include "lexer.h"
#include "compiler_lexer_Lexer.h"
#include <iostream>
#include <cstring>

// External functions from Flex-generated lexer (fallback)
extern int yylex();
extern char* yytext;
extern int yylineno;
extern FILE* yyin;

// Global JNI state (from jni_lexer.cpp)
extern JavaVM* jvm;
extern jobject globalLexerObj;
extern jmethodID nextTokenMethod;
extern jmethodID getLexemeMethod;
extern jmethodID getTypeMethod;
extern jmethodID getLineMethod;

JavaLexer::JavaLexer() {
    std::cout << "JavaLexer initialized" << std::endl;
}

JavaLexer::~JavaLexer() {
    std::cout << "JavaLexer destroyed" << std::endl;
}

// JNI-based token retrieval
int JavaLexer::nextToken() {
    if (jvm && globalLexerObj && nextTokenMethod) {
        // Attach to current thread
        JNIEnv* env;
        if (jvm->AttachCurrentThread((void**)&env, nullptr) != 0) {
            std::cerr << "Failed to attach to JVM" << std::endl;
            return 309; // EOF
        }

        // Call Java method
        jint tokenType = env->CallIntMethod(globalLexerObj, nextTokenMethod);

        // Check for exceptions
        if (env->ExceptionCheck()) {
            std::cerr << "Exception in nextToken" << std::endl;
            env->ExceptionDescribe();
            env->ExceptionClear();
            tokenType = 309; // EOF on error
        }

        // Detach from thread
        jvm->DetachCurrentThread();

        lastToken = tokenType;
        return tokenType;
    } else {
        // Fallback to Flex lexer
        return fallbackNextToken();
    }
}

const char* JavaLexer::getLexeme() {
    if (jvm && globalLexerObj && getLexemeMethod) {
        // Attach to current thread
        JNIEnv* env;
        if (jvm->AttachCurrentThread((void**)&env, nullptr) != 0) {
            std::cerr << "Failed to attach to JVM" << std::endl;
            return "";
        }

        // Call Java method
        jstring lexeme = (jstring)env->CallObjectMethod(globalLexerObj, getLexemeMethod);
        const char* result = nullptr;

        if (!env->ExceptionCheck() && lexeme) {
            result = env->GetStringUTFChars(lexeme, nullptr);
            lastLexeme = result ? result : "";
            env->ReleaseStringUTFChars(lexeme, result);
        } else {
            if (env->ExceptionCheck()) {
                std::cerr << "Exception in getLexeme" << std::endl;
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
            lastLexeme = "";
        }

        // Detach from thread
        jvm->DetachCurrentThread();

        return lastLexeme.c_str();
    } else {
        // Fallback to Flex lexer
        return fallbackGetLexeme();
    }
}

int JavaLexer::getType() {
    return lastToken;
}

int JavaLexer::getLine() {
    if (jvm && globalLexerObj && getLineMethod) {
        // Attach to current thread
        JNIEnv* env;
        if (jvm->AttachCurrentThread((void**)&env, nullptr) != 0) {
            std::cerr << "Failed to attach to JVM" << std::endl;
            return 0;
        }

        // Call Java method
        jint line = env->CallIntMethod(globalLexerObj, getLineMethod);

        // Check for exceptions
        if (env->ExceptionCheck()) {
            std::cerr << "Exception in getLine" << std::endl;
            env->ExceptionDescribe();
            env->ExceptionClear();
            line = 0;
        }

        // Detach from thread
        jvm->DetachCurrentThread();

        lastLine = line;
        return line;
    } else {
        // Fallback to Flex lexer
        return fallbackGetLine();
    }
}

// Fallback methods for Flex lexer
int JavaLexer::fallbackNextToken() {
    int token = yylex();
    lastToken = token;
    lastLexeme = yytext ? yytext : "";
    lastLine = yylineno;
    return token;
}

const char* JavaLexer::fallbackGetLexeme() {
    return lastLexeme.c_str();
}

int JavaLexer::fallbackGetLine() {
    return lastLine;
}

// Set input file for Flex lexer
void JavaLexer::setInputFile(const char* filename) {
    yyin = fopen(filename, "r");
    if (!yyin) {
        std::cerr << "Error opening file: " << filename << std::endl;
    }
}