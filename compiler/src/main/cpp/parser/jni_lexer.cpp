#include <jni.h>
#include <iostream>
#include "lexer.h"

// Global references for JNI
static JavaVM* jvm = nullptr;
static jobject globalLexerObj = nullptr;
static jmethodID setInputMethod = nullptr;
static jmethodID nextTokenMethod = nullptr;
static jmethodID getLexemeMethod = nullptr;
static jmethodID getTypeMethod = nullptr;
static jmethodID getLineMethod = nullptr;

// JNI function to initialize parser
extern "C" JNIEXPORT void JNICALL Java_compiler_lexer_Lexer_initializeParser
  (JNIEnv *env, jobject obj) {
    std::cout << "JNI: Initializing parser with Java lexer integration" << std::endl;

    // Store JVM reference
    env->GetJavaVM(&jvm);

    // Create global reference to lexer object
    globalLexerObj = env->NewGlobalRef(obj);

    // Get method IDs
    jclass lexerClass = env->GetObjectClass(obj);
    setInputMethod = env->GetMethodID(lexerClass, "setInputForJNI", "(Ljava/lang/String;)V");
    nextTokenMethod = env->GetMethodID(lexerClass, "nextTokenJNI", "()I");
    getLexemeMethod = env->GetMethodID(lexerClass, "getLexemeJNI", "()Ljava/lang/String;");
    getTypeMethod = env->GetMethodID(lexerClass, "getTypeJNI", "()I");
    getLineMethod = env->GetMethodID(lexerClass, "getLineJNI", "()I");

    if (!setInputMethod || !nextTokenMethod || !getLexemeMethod || !getTypeMethod || !getLineMethod) {
        std::cerr << "JNI: Failed to get method IDs" << std::endl;
        return;
    }

    std::cout << "JNI: Java lexer integration initialized successfully" << std::endl;
}

// JNI function to parse input
extern "C" JNIEXPORT jboolean JNICALL Java_compiler_lexer_Lexer_parseInput
  (JNIEnv *env, jobject obj, jstring input) {
    std::cout << "JNI: Setting input for parsing" << std::endl;

    if (!globalLexerObj) {
        std::cerr << "JNI: Lexer not initialized" << std::endl;
        return JNI_FALSE;
    }

    // Call setInputForJNI on the Java lexer
    env->CallVoidMethod(globalLexerObj, setInputMethod, input);

    // Check for exceptions
    if (env->ExceptionCheck()) {
        std::cerr << "JNI: Exception in setInputForJNI" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return JNI_FALSE;
    }

    std::cout << "JNI: Input set successfully" << std::endl;
    return JNI_TRUE;
}

// JNI function to get next token
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_nextTokenJNI
  (JNIEnv *env, jobject obj) {
    if (!globalLexerObj) {
        std::cerr << "JNI: Lexer not initialized for nextToken" << std::endl;
        return 309; // EOF token
    }

    // Call nextTokenJNI on Java lexer
    jint tokenType = env->CallIntMethod(globalLexerObj, nextTokenMethod);

    // Check for exceptions
    if (env->ExceptionCheck()) {
        std::cerr << "JNI: Exception in nextTokenJNI" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return 309; // EOF on error
    }

    return tokenType;
}

// JNI function to get lexeme
extern "C" JNIEXPORT jstring JNICALL Java_compiler_lexer_Lexer_getLexemeJNI
  (JNIEnv *env, jobject obj) {
    if (!globalLexerObj) {
        return env->NewStringUTF("");
    }

    // Call getLexemeJNI on Java lexer
    jstring lexeme = (jstring)env->CallObjectMethod(globalLexerObj, getLexemeMethod);

    // Check for exceptions
    if (env->ExceptionCheck()) {
        std::cerr << "JNI: Exception in getLexemeJNI" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return env->NewStringUTF("");
    }

    return lexeme;
}

// JNI function to get token type
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_getTypeJNI
  (JNIEnv *env, jobject obj) {
    if (!globalLexerObj) {
        return 309; // EOF
    }

    // Call getTypeJNI on Java lexer
    jint tokenType = env->CallIntMethod(globalLexerObj, getTypeMethod);

    // Check for exceptions
    if (env->ExceptionCheck()) {
        std::cerr << "JNI: Exception in getTypeJNI" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return 309; // EOF on error
    }

    return tokenType;
}

// JNI function to get line number
extern "C" JNIEXPORT jint JNICALL Java_compiler_lexer_Lexer_getLineJNI
  (JNIEnv *env, jobject obj) {
    if (!globalLexerObj) {
        return 0;
    }

    // Call getLineJNI on Java lexer
    jint line = env->CallIntMethod(globalLexerObj, getLineMethod);

    // Check for exceptions
    if (env->ExceptionCheck()) {
        std::cerr << "JNI: Exception in getLineJNI" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return 0;
    }

    return line;
}
