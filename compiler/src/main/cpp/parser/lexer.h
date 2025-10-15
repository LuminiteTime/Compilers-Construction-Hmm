#ifndef LEXER_H
#define LEXER_H

#include <jni.h>
#include <string>

// Java lexer integration class
class JavaLexer {
private:
    JNIEnv* env;
    jobject lexerInstance;
    jmethodID nextTokenMethod;
    jmethodID getTypeMethod;
    jmethodID getLexemeMethod;
    jmethodID getLineMethod;

    // For testing with Flex lexer
    int lastToken;
    std::string lastLexeme;
    int lastLine;

public:
    JavaLexer();
    ~JavaLexer();

    // Methods to interface with Java lexer
    int nextToken();
    const char* getLexeme();
    int getType();
    int getLine();

    // For testing with Flex lexer
    void setInputFile(const char* filename);
};

// External functions for Bison
extern int yylex();
extern char* yytext;
extern int yylineno;

#endif // LEXER_H
