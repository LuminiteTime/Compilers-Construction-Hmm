package com.languagei.compiler.lexer;

/**
 * Represents a position in source code
 */
public class Position {
    private final int line;
    private final int column;
    private final int offset;
    private final String filename;

    public Position(int line, int column, int offset, String filename) {
        this.line = line;
        this.column = column;
        this.offset = offset;
        this.filename = filename;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public int getOffset() {
        return offset;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return filename + ":" + line + ":" + column;
    }
}

