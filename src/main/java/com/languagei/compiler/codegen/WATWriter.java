package com.languagei.compiler.codegen;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes WebAssembly Text (WAT) format code
 */
public class WATWriter {
    private final Writer writer;
    private int indentLevel = 0;
    private static final String INDENT = "  ";

    public WATWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(String text) throws IOException {
        writer.write(text);
    }

    public void writeLine(String text) throws IOException {
        writeIndent();
        writer.write(text);
        writer.write("\n");
    }

    public void writeIndent() throws IOException {
        for (int i = 0; i < indentLevel; i++) {
            writer.write(INDENT);
        }
    }

    public void indent() {
        indentLevel++;
    }

    public void dedent() {
        if (indentLevel > 0) indentLevel--;
    }

    public void writeOpenParen(String name) throws IOException {
        writeIndent();
        writer.write("(");
        writer.write(name);
        writer.write("\n");
        indent();
    }

    public void writeCloseParen() throws IOException {
        dedent();
        writeIndent();
        writer.write(")\n");
    }

    public void close() throws IOException {
        writer.close();
    }

    public void flush() throws IOException {
        writer.flush();
    }
}

