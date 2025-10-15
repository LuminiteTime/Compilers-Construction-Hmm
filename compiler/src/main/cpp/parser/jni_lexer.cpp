#include <jni.h>
#include <iostream>
#include "lexer.h"

// Global lexer instance for JNI
static JavaLexer* globalLexer = nullptr;

// JNI function to initialize parser
extern "C" JNIEXPORT void JNICALL Java_compiler_lexer_Lexer_initializeParser
  (JNIEnv *env, jobject obj) {
    std::cout << "JNI: Initializing parser" << std::endl;
    if (globalLexer) {
        delete globalLexer;
    }
    globalLexer = new JavaLexer();
}

// JNI function to parse input
extern "C" JNIEXPORT jboolean JNICALL Java_compiler_lexer_Lexer_parseInput
  (JNIEnv *env, jobject obj, jstring input) {
    std::cout << "JNI: Parsing input" << std::endl;
    // For now, just return true - full implementation would parse the string
    return JNI_TRUE;
}

// JNI function to get next token (for testing)
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_nextTokenJNI
  (JNIEnv *env, jobject obj) {
    if (globalLexer) {
        return globalLexer->nextToken();
    }
    return 0; // EOF
}

// JNI function to get lexeme
extern "C" JNIEXPORT jstring JNICALL Java_compiler_lexer_Lexer_getLexemeJNI
  (JNIEnv *env, jobject obj) {
    if (globalLexer) {
        const char* lexeme = globalLexer->getLexeme();
        return env->NewStringUTF(lexeme);
    }
    return env->NewStringUTF("");
}

// JNI function to get token type
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_getTypeJNI
  (JNIEnv *env, jobject obj) {
    if (globalLexer) {
        return globalLexer->getType();
    }
    return 0;
}

// JNI function to get line number
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_getLineJNI
  (JNIEnv *env, jobject obj) {
    if (globalLexer) {
        return globalLexer->getLine();
    }
    return 0;
}
