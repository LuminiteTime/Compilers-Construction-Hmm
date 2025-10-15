%{
#include <iostream>
#include <string>
#include <cstdlib>
#include "ast.h"      // AST node definitions
#include "parser.tab.h"

// YYSTYPE is defined in parser.tab.h

// Flex global variables
extern char* yytext;
extern int yylineno;
%}

%option noyywrap

%%

[ \t]           ;  // ignore whitespace
\n              { yylineno++; }

"var"           { return TOK_VAR; }
"type"          { return TOK_TYPE; }
"is"            { return TOK_IS; }
"integer"       { return TOK_INTEGER; }
"real"          { return TOK_REAL; }
"boolean"       { return TOK_BOOLEAN; }
"array"         { return TOK_ARRAY; }
"record"        { return TOK_RECORD; }
"end"           { return TOK_END; }
"while"         { return TOK_WHILE; }
"loop"          { return TOK_LOOP; }
"for"           { return TOK_FOR; }
"in"            { return TOK_IN; }
"reverse"       { return TOK_REVERSE; }
"if"            { return TOK_IF; }
"then"          { return TOK_THEN; }
"else"          { return TOK_ELSE; }
"print"         { return TOK_PRINT; }
"routine"       { return TOK_ROUTINE; }
"true"          { return TOK_TRUE; }
"false"         { return TOK_FALSE; }
"and"           { return TOK_AND; }
"or"            { return TOK_OR; }
"xor"           { return TOK_XOR; }
"not"           { return TOK_NOT; }

":="            { return TOK_ASSIGN; }
".."            { return TOK_DOTDOT; }
"+"             { return TOK_PLUS; }
"-"             { return TOK_MINUS; }
"*"             { return TOK_MULTIPLY; }
"/"             { return TOK_DIVIDE; }
"%"             { return TOK_MODULO; }
"<"             { return TOK_LESS; }
"<="            { return TOK_LESS_EQUAL; }
">"             { return TOK_GREATER; }
">="            { return TOK_GREATER_EQUAL; }
"="             { return TOK_EQUAL; }
"/="            { return TOK_NOT_EQUAL; }

":"             { return TOK_COLON; }
";"             { return TOK_SEMICOLON; }
","             { return TOK_COMMA; }
"."             { return TOK_DOT; }
"("             { return TOK_LPAREN; }
")"             { return TOK_RPAREN; }
"["             { return TOK_LBRACKET; }
"]"             { return TOK_RBRACKET; }
"=>"            { return TOK_ARROW; }

[0-9]+          { yylval.intVal = atoi(yytext); return TOK_INTEGER_LITERAL; }
[0-9]+\.[0-9]+  { yylval.realVal = atof(yytext); return TOK_REAL_LITERAL; }
\"[^\"]*\"       { yylval.strVal = strdup(yytext); return TOK_STRING_LITERAL; }  // String literals
[a-zA-Z_][a-zA-Z0-9_]* { yylval.strVal = strdup(yytext); return TOK_IDENTIFIER; }

<<EOF>>         { return 0; }

.               { std::cerr << "Unknown character: " << yytext << std::endl; }

%%
