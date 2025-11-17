package compiler.codegen;

/**
 * Pretty-printer for WebAssembly text format
 */
public class WasmPrinter {
    private StringBuilder output;
    private int indentLevel = 0;
    private static final String INDENT = "  ";

    public WasmPrinter() {
        this.output = new StringBuilder();
    }

    /**
     * Emit line with automatic indentation
     */
    public void writeLine(String line) {
        writeIndent();
        output.append(line).append("\n");
    }

    /**
     * Emit inline code without newline
     */
    public void write(String code) {
        output.append(code);
    }

    /**
     * Emit newline
     */
    public void newline() {
        output.append("\n");
    }

    /**
     * Increase indentation
     */
    public void indent() {
        indentLevel++;
    }

    /**
     * Decrease indentation
     */
    public void dedent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }

    /**
     * Write current indentation
     */
    private void writeIndent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append(INDENT);
        }
    }

    /**
     * Get generated output
     */
    public String toString() {
        return output.toString();
    }

    /**
     * Clear output
     */
    public void clear() {
        output = new StringBuilder();
        indentLevel = 0;
    }

    /**
     * Emit module start
     */
    public void startModule() {
        writeLine("(module");
        indent();
    }

    /**
     * Emit module end
     */
    public void endModule() {
        dedent();
        writeLine(")");
    }

    /**
     * Emit function start
     */
    public void startFunction(String name, String parameters, String returnType) {
        StringBuilder sb = new StringBuilder("(func $");
        sb.append(name);
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(" ").append(parameters);
        }
        if (returnType != null && !returnType.isEmpty()) {
            sb.append(" (result ").append(returnType).append(")");
        }
        writeLine(sb.toString());
        indent();
    }

    /**
     * Emit function end
     */
    public void endFunction() {
        dedent();
        writeLine(")");
    }

    /**
     * Emit local variable declaration
     */
    public void declareLocal(String name, String type) {
        writeLine("(local $" + name + " " + type + ")");
    }

    /**
     * Emit global variable declaration
     */
    public void declareGlobal(String name, String type, String initialValue) {
        writeLine("(global $" + name + " (mut " + type + ") (" + type + ".const " + initialValue + "))");
    }

    /**
     * Emit memory declaration
     */
    public void declareMemory(int pages) {
        writeLine("(memory " + pages + ")");
    }

    /**
     * Emit export
     */
    public void export(String name, String kind, String item) {
        writeLine("(export \"" + name + "\" (" + kind + " $" + item + "))");
    }

    /**
     * Emit import
     */
    public void importFunc(String module, String name, String funcName, String signature) {
        writeLine("(import \"" + module + "\" \"" + name + "\"\n" +
                  "  (func $" + funcName + " " + signature + "))");
    }

    /**
     * Emit instruction
     */
    public void instruction(String instr) {
        writeLine(instr);
    }

    /**
     * Emit instruction with argument
     */
    public void instructionWithArg(String instr, String arg) {
        writeLine(instr + " " + arg);
    }

    /**
     * Emit block start
     */
    public void startBlock(String label) {
        writeLine("(block $" + label);
        indent();
    }

    /**
     * Emit block end
     */
    public void endBlock() {
        dedent();
        writeLine(")");
    }

    /**
     * Emit loop start
     */
    public void startLoop(String label) {
        writeLine("(loop $" + label);
        indent();
    }

    /**
     * Emit loop end
     */
    public void endLoop() {
        dedent();
        writeLine(")");
    }

    /**
     * Emit if start
     */
    public void startIf(String resultType) {
        if (resultType != null && !resultType.isEmpty()) {
            writeLine("(if (result " + resultType + ")");
        } else {
            writeLine("(if");
        }
        indent();
    }

    /**
     * Emit else
     */
    public void startElse() {
        dedent();
        writeLine("else");
        indent();
    }

    /**
     * Emit if end
     */
    public void endIf() {
        dedent();
        writeLine(")");
    }

    /**
     * Emit constant
     */
    public void constant(String type, String value) {
        writeLine(type + ".const " + value);
    }

    /**
     * Emit variable load
     */
    public void localGet(String name) {
        writeLine("local.get $" + name);
    }

    /**
     * Emit global load
     */
    public void globalGet(String name) {
        writeLine("global.get $" + name);
    }

    /**
     * Emit variable store
     */
    public void localSet(String name) {
        writeLine("local.set $" + name);
    }

    /**
     * Emit global store
     */
    public void globalSet(String name) {
        writeLine("global.set $" + name);
    }

    /**
     * Emit function call
     */
    public void call(String name) {
        writeLine("call $" + name);
    }

    /**
     * Emit branch
     */
    public void br(String label) {
        writeLine("br $" + label);
    }

    /**
     * Emit conditional branch
     */
    public void brIf(String label) {
        writeLine("br_if $" + label);
    }

    /**
     * Emit return
     */
    public void ret() {
        writeLine("return");
    }

    /**
     * Emit binary operation
     */
    public void binOp(String op) {
        writeLine(op);
    }

    /**
     * Emit unary operation
     */
    public void unOp(String op) {
        writeLine(op);
    }

    /**
     * Emit comment
     */
    public void comment(String text) {
        writeLine(";; " + text);
    }

    /**
     * Get indentation level
     */
    public int getIndentLevel() {
        return indentLevel;
    }

    /**
     * Format complete WAT module as string
     */
    public String formatModule() {
        return output.toString();
    }
}

