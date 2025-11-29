package com.languagei.compiler.lexer;

/**
 * Enumeration of all token types in Language I
 */
public enum TokenType {
    // Keywords - Declarations
    VAR, TYPE, ROUTINE, IS, END, RECORD, ARRAY,

    // Keywords - Types
    INTEGER_TYPE, REAL_TYPE, BOOLEAN_TYPE, TRUE, FALSE,

    // Keywords - Control flow
    IF, THEN, ELSE, WHILE, LOOP, FOR, IN, REVERSE, RETURN,

    // Keywords - Special
    PRINT,

    // Operators - Arithmetic
    PLUS, MINUS, STAR, SLASH, PERCENT,

    // Operators - Logical
    AND, OR, XOR, NOT,

    // Operators - Comparison
    LT, LE, GT, GE, EQ, NE,

    // Operators - Assignment and arrows
    ASSIGN, ARROW,

    // Delimiters
    LPAREN, RPAREN, LBRACKET, RBRACKET, DOT, COLON, COMMA, RANGE,

    // Literals and identifiers
    INTEGER_LITERAL, REAL_LITERAL, IDENTIFIER,

    // Special
    EOF, NEWLINE, SEMICOLON;

    @Override
    public String toString() {
        return this.name();
    }
}

