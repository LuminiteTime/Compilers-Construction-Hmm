package com.languagei.compiler.semantic;

import com.languagei.compiler.lexer.Position;

/**
 * Represents a compilation error or warning
 */
public class CompilationError {
    public enum Severity {
        ERROR, WARNING, INFO
    }

    private final Severity severity;
    private final String message;
    private final Position position;

    public CompilationError(Severity severity, String message, Position position) {
        this.severity = severity;
        this.message = message;
        this.position = position;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return String.format("%s: %s at %s", severity, message, position);
    }
}

