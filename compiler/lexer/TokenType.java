/**
 * Token types for the Imperative (I) language lexer.
 * Each token type corresponds to a terminal symbol in the language grammar.
 */
public enum TokenType {
    // Keywords - Declarations
    VAR, TYPE, IS, INTEGER, REAL, BOOLEAN, ARRAY, RECORD, END,

    // Keywords - Statements
    WHILE, LOOP, FOR, IN, REVERSE, IF, THEN, ELSE, PRINT, ROUTINE,

    // Keywords - Expressions/Booleans
    TRUE, FALSE, AND, OR, XOR, NOT,

    // Keywords - Other
    RETURN,

    // Literals
    IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL, STRING_LITERAL,

    // Operators - Arithmetic
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,

    // Operators - Relational
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,

    // Operators - Assignment and Range
    ASSIGN, RANGE,

    // Delimiters/Punctuation
    COLON, SEMICOLON, COMMA, DOT, LPAREN, RPAREN, LBRACKET, RBRACKET,

    // Special tokens
    EOF
}
