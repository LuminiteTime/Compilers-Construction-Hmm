package com.languagei.compiler.semantic;

/**
 * Represents a symbol (variable, type, or function) in the symbol table
 */
public class Symbol {
    public enum Kind {
        VARIABLE, TYPE, FUNCTION
    }

    private final String name;
    private final Kind kind;
    private final Type type;

    public Symbol(String name, Kind kind, Type type) {
        this.name = name;
        this.kind = kind;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Symbol(%s, %s, %s)", name, kind, type.getName());
    }
}

