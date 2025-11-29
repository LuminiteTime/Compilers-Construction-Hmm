package com.languagei.compiler.lexer;

/**
 * Exception thrown by lexer for lexical errors
 */
public class LexerException extends RuntimeException {
    public LexerException(String message) {
        super(message);
    }

    public LexerException(String message, Throwable cause) {
        super(message, cause);
    }
}

