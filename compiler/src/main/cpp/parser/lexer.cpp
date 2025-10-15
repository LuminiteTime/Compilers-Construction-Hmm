#include "lexer.h"
#include <iostream>
#include <cstring>

// External functions from Flex-generated lexer
extern int yylex();
extern char* yytext;
extern int yylineno;
extern FILE* yyin;

JavaLexer::JavaLexer() {
    std::cout << "JavaLexer initialized" << std::endl;
}

JavaLexer::~JavaLexer() {
    std::cout << "JavaLexer destroyed" << std::endl;
}

// For testing with Flex lexer
int JavaLexer::nextToken() {
    int token = yylex();
    lastToken = token;
    lastLexeme = yytext ? yytext : "";
    lastLine = yylineno;
    return token;
}

const char* JavaLexer::getLexeme() {
    return lastLexeme.c_str();
}

int JavaLexer::getType() {
    return lastToken;
}

int JavaLexer::getLine() {
    return lastLine;
}

// Set input file for Flex lexer
void JavaLexer::setInputFile(const char* filename) {
    yyin = fopen(filename, "r");
    if (!yyin) {
        std::cerr << "Error opening file: " << filename << std::endl;
    }
}
