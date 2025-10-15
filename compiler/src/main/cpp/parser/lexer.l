%{
#include <iostream>
#include <string>
#include "parser.tab.h"

// Global variables for yytext and yylineno
char* yytext;
int yylineno = 1;
%}

%option noyywrap

%%

[ \t]           ;  // ignore whitespace
\n              { yylineno++; }

"var"           { return VAR; }
"type"          { return TYPE; }
"is"            { return IS; }
"integer"       { return INTEGER; }
"real"          { return REAL; }
"boolean"       { return BOOLEAN; }
"array"         { return ARRAY; }
"record"        { return RECORD; }
"end"           { return END; }
"while"         { return WHILE; }
"loop"          { return LOOP; }
"for"           { return FOR; }
"in"            { return IN; }
"reverse"       { return REVERSE; }
"if"            { return IF; }
"then"          { return THEN; }
"else"          { return ELSE; }
"print"         { return PRINT; }
"routine"       { return ROUTINE; }
"true"          { return TRUE; }
"false"         { return FALSE; }
"and"           { return AND; }
"or"            { return OR; }
"xor"           { return XOR; }
"not"           { return NOT; }

":="            { return ASSIGN; }
".."            { return DOTDOT; }
"+"             { return PLUS; }
"-"             { return MINUS; }
"*"             { return MUL; }
"/"             { return DIV; }
"%"             { return MOD; }
"<"             { return LT; }
"<="            { return LE; }
">"             { return GT; }
">="            { return GE; }
"="             { return EQ; }
"/="            { return NE; }

":"             { return COLON; }
","             { return COMMA; }
"."             { return DOT; }
"("             { return LPAREN; }
")"             { return RPAREN; }
"["             { return LBRACKET; }
"]"             { return RBRACKET; }
"=>"            { return ARROW; }

[0-9]+          { yylval.intVal = atoi(yytext); return INTEGER_LITERAL; }
[0-9]+\.[0-9]+  { yylval.realVal = atof(yytext); return REAL_LITERAL; }
[a-zA-Z_][a-zA-Z0-9_]* { yylval.strVal = strdup(yytext); return IDENTIFIER; }

.               { std::cerr << "Unknown character: " << yytext << std::endl; }

%%