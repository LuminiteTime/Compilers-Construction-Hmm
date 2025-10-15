#ifndef LEXER_H
#define LEXER_H

#include <jni.h>

// Stub for Java lexer integration via JNI
class JavaLexer {
private:
    JNIEnv* env;
    jobject lexerInstance;
    jmethodID nextTokenMethod;
    jmethodID getTypeMethod;
    jmethodID getLexemeMethod;
    jmethodID getLineMethod;

public:
    JavaLexer();
    ~JavaLexer();

    // Methods to interface with Java lexer
    int nextToken();
    const char* getLexeme();
    int getType();
    int getLine();
};

// External functions for Bison
extern int yylex();
extern char* yytext;
extern int yylineno;

#endif // LEXER_H