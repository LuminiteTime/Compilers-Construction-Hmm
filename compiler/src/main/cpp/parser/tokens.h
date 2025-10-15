#ifndef TOKENS_H
#define TOKENS_H

// Token definitions for the Imperative (I) language lexer and parser
// These constants must match the %token declarations in parser.y

// Literals
#define TOK_IDENTIFIER 258
#define TOK_INTEGER_LITERAL 259
#define TOK_REAL_LITERAL 260

// Keywords
#define TOK_VAR 261
#define TOK_TYPE 262
#define TOK_IS 263
#define TOK_INTEGER 264
#define TOK_REAL 265
#define TOK_BOOLEAN 266
#define TOK_ARRAY 267
#define TOK_RECORD 268
#define TOK_END 269
#define TOK_WHILE 270
#define TOK_LOOP 271
#define TOK_FOR 272
#define TOK_IN 273
#define TOK_REVERSE 274
#define TOK_IF 275
#define TOK_THEN 276
#define TOK_ELSE 277
#define TOK_PRINT 278
#define TOK_ROUTINE 279
#define TOK_TRUE 280
#define TOK_FALSE 281
#define TOK_AND 282
#define TOK_OR 283
#define TOK_XOR 284
#define TOK_NOT 285

// Operators
#define TOK_ASSIGN 286
#define TOK_DOTDOT 287
#define TOK_PLUS 288
#define TOK_MINUS 289
#define TOK_MULTIPLY 290
#define TOK_DIVIDE 291
#define TOK_MODULO 292
#define TOK_LESS 293
#define TOK_LESS_EQUAL 294
#define TOK_GREATER 295
#define TOK_GREATER_EQUAL 296
#define TOK_EQUAL 297
#define TOK_NOT_EQUAL 298

// Delimiters
#define TOK_COLON 299
#define TOK_SEMICOLON 300
#define TOK_COMMA 301
#define TOK_DOT 302
#define TOK_LPAREN 303
#define TOK_RPAREN 304
#define TOK_LBRACKET 305
#define TOK_RBRACKET 306
#define TOK_ARROW 307

// Special tokens
#define TOK_EOF_TOKEN 308

#endif // TOKENS_H
