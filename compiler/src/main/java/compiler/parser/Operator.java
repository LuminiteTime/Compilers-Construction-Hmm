package compiler.parser;

public enum Operator {
    // Binary operators
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,
    AND, OR, XOR,
    // Unary operators
    NOT, UNARY_PLUS, UNARY_MINUS
}