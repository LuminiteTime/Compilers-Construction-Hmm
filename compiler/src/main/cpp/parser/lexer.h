#ifndef LEXER_H
#define LEXER_H

#include <jni.h>
#include <string>

// Java lexer integration class
class JavaLexer {
private:
    // JNI environment and methods are now global in jni_lexer.cpp
    // This class now acts as an interface to the global JNI state

    // For testing with Flex lexer (fallback)
    int lastToken;
    std::string lastLexeme;
    int lastLine;

public:
    JavaLexer();
    ~JavaLexer();

    // Methods to interface with Java lexer via JNI
    int nextToken();
    const char* getLexeme();
    int getType();
    int getLine();

    // For testing with Flex lexer (fallback when JNI not available)
    void setInputFile(const char* filename);
};

// External functions for Bison
extern int yylex();
extern char* yytext;
extern int yylineno;

#endif // LEXER_H
