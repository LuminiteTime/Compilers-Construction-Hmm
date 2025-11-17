package compiler.codegen;

import java.util.*;

/**
 * Main WebAssembly code generator
 * Generates WAT (WebAssembly Text format) from AST
 */
public class WasmCodeGenerator {
    private CodeGenSymbolTable symbolTable;
    private StringBuilder watOutput;
    private int blockDepth = 0;
    private int loopDepth = 0;
    private int indentLevel = 0;

    // Global state
    private int memoryPages = 1;
    private boolean hasMemory = false;
    private boolean hasHeapPtr = false;
    private List<String> functions = new ArrayList<>();
    private List<String> globals = new ArrayList<>();
    private List<String> imports = new ArrayList<>();

    public WasmCodeGenerator() {
        this.symbolTable = new CodeGenSymbolTable();
        this.watOutput = new StringBuilder();
        initializeWasiImports();
    }

    public WasmCodeGenerator(CodeGenSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.watOutput = new StringBuilder();
        initializeWasiImports();
    }

    /**
     * Initialize WASI imports for I/O
     */
    private void initializeWasiImports() {
        imports.add("(import \"wasi_snapshot_preview1\" \"fd_write\"\n" +
                "  (func $fd_write (param i32 i32 i32 i32) (result i32)))");
    }

    /**
     * Generate WASM module from program AST
     * Note: This is a placeholder - actual implementation depends on C++ parser AST structure
     */
    public String generate(Object programAst) throws CodeGenException {
        watOutput = new StringBuilder();
        watOutput.append("(module\n");

        // Add imports
        for (String imp : imports) {
            watOutput.append("  ").append(imp).append("\n");
        }

        // Add memory
        watOutput.append("  (memory ").append(memoryPages).append(")\n");
        watOutput.append("  (export \"memory\" (memory 0))\n");

        // Add global heap pointer
        watOutput.append("  (global $heap_ptr (mut i32) (i32.const 0x1000))\n");

        // Add allocation function
        emitAllocFunction();

        // Add print functions
        emitPrintFunctions();

        // Add other declarations (would be generated from AST)

        watOutput.append(")\n");

        return watOutput.toString();
    }

    /**
     * Generate WASM module as binary
     */
    public byte[] generateBinary(Object programAst) throws CodeGenException {
        String wat = generate(programAst);
        // This would require external tool integration (WABT)
        throw new CodeGenException("Binary generation requires external WABT tool");
    }

    /**
     * Emit allocation function for dynamic memory
     */
    private void emitAllocFunction() {
        watOutput.append("  (func $alloc (param $size i32) (result i32)\n");
        watOutput.append("    (local $ptr i32)\n");
        watOutput.append("    global.get $heap_ptr\n");
        watOutput.append("    local.set $ptr\n");
        watOutput.append("    local.get $ptr\n");
        watOutput.append("    local.get $size\n");
        watOutput.append("    i32.add\n");
        watOutput.append("    global.set $heap_ptr\n");
        watOutput.append("    local.get $ptr\n");
        watOutput.append("  )\n");
    }

    /**
     * Emit print functions
     */
    private void emitPrintFunctions() {
        // Print integer
        watOutput.append("  (func $print_int (param $value i32)\n");
        watOutput.append("    ;; Convert i32 to string and print using fd_write\n");
        watOutput.append("    ;; For now, simplified implementation\n");
        watOutput.append("    local.get $value\n");
        watOutput.append("    i32.const 0\n");
        watOutput.append("    i32.le_s\n");
        watOutput.append("    if\n");
        watOutput.append("      ;; Handle negative numbers\n");
        watOutput.append("    else\n");
        watOutput.append("      ;; Handle positive numbers\n");
        watOutput.append("    end\n");
        watOutput.append("  )\n");

        // Print real
        watOutput.append("  (func $print_real (param $value f64)\n");
        watOutput.append("    ;; Convert f64 to string and print\n");
        watOutput.append("  )\n");
    }

    /**
     * Helper: emit line with indentation
     */
    private void emit(String code) {
        for (int i = 0; i < indentLevel; i++) {
            watOutput.append("  ");
        }
        watOutput.append(code).append("\n");
    }

    /**
     * Helper: emit code without newline
     */
    private void emitInline(String code) {
        watOutput.append(code);
    }

    /**
     * Get generated WAT code
     */
    public String getWat() {
        return watOutput.toString();
    }

    /**
     * Write output to file
     */
    public void writeToFile(String filename) throws Exception {
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            watOutput.toString()
        );
    }

    /**
     * Get symbol table
     */
    public CodeGenSymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Enter a new scope
     */
    public void enterScope() {
        symbolTable.enterScope();
    }

    /**
     * Exit current scope
     */
    public void exitScope() {
        symbolTable.exitScope();
    }

    /**
     * Map language type to WASM type string
     */
    public static String languageTypeToWasm(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer" -> "i32";
            case "real" -> "f64";
            case "boolean" -> "i32";  // 0 = false, 1 = true
            case "array", "record" -> "i32";  // pointer to memory
            default -> throw new CodeGenException("Unknown language type: " + langType);
        };
    }

    /**
     * Calculate size of type in bytes
     */
    public static int getTypeSize(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer", "boolean" -> 4;
            case "real" -> 8;
            case "array", "record" -> 4;  // pointer size
            default -> throw new CodeGenException("Unknown type size for: " + langType);
        };
    }

    /**
     * Emit type conversion code if needed
     */
    public void emitTypeConversion(String fromType, String toType) {
        if (fromType.equalsIgnoreCase(toType)) {
            return;  // No conversion needed
        }

        if (fromType.equalsIgnoreCase("integer") && toType.equalsIgnoreCase("real")) {
            emit("f64.convert_i32_s  ;; i32 to f64");
        } else if (fromType.equalsIgnoreCase("real") && toType.equalsIgnoreCase("integer")) {
            emit("i32.trunc_f64_s  ;; f64 to i32");
        }
    }

    /**
     * Emit a constant value
     */
    public void emitConstant(String type, String value) {
        String wasmType = languageTypeToWasm(type);
        if (wasmType.equals("i32")) {
            emit(wasmType + ".const " + value);
        } else if (wasmType.equals("f64")) {
            emit(wasmType + ".const " + value);
        }
    }

    /**
     * Emit local variable declaration
     */
    public void emitLocalDeclaration(String name, String type) {
        symbolTable.declareLocal(name, type);
        String wasmType = languageTypeToWasm(type);
        emit("(local $" + name + " " + wasmType + ")");
    }

    /**
     * Emit global variable declaration
     */
    public void emitGlobalDeclaration(String name, String type, String initialValue) {
        symbolTable.declareGlobal(name, type);
        String wasmType = languageTypeToWasm(type);
        globals.add("(global $" + name + " (mut " + wasmType + ") (" + wasmType + ".const " + initialValue + "))");
    }

    /**
     * Emit variable load
     */
    public void emitLoad(String name) {
        SymbolInfo info = symbolTable.lookup(name);
        if (info == null) {
            throw new CodeGenException("Undefined variable: " + name);
        }

        if (info.getKind() == SymbolInfo.SymbolKind.GLOBAL) {
            emit("global.get $" + name);
        } else {
            emit("local.get $" + name);
        }
    }

    /**
     * Emit variable store
     */
    public void emitStore(String name) {
        SymbolInfo info = symbolTable.lookup(name);
        if (info == null) {
            throw new CodeGenException("Undefined variable: " + name);
        }

        if (info.getKind() == SymbolInfo.SymbolKind.GLOBAL) {
            emit("global.set $" + name);
        } else {
            emit("local.set $" + name);
        }
    }

    /**
     * Emit function call
     */
    public void emitFunctionCall(String funcName) {
        emit("call $" + funcName);
    }

    /**
     * Emit binary operation
     */
    public void emitBinaryOp(String op, String operandType) {
        String wasmOp = WasmOperator.getBinaryOp(op, operandType);
        emit(wasmOp);
    }

    /**
     * Emit unary operation
     */
    public void emitUnaryOp(String op, String operandType) {
        String wasmOp = WasmOperator.getUnaryOp(op, operandType);
        if (!wasmOp.isEmpty()) {
            for (String line : wasmOp.split("\n")) {
                emit(line);
            }
        }
    }

    /**
     * Emit if-then-else block
     */
    public void emitIfStart(String resultType) {
        if (resultType == null || resultType.isEmpty()) {
            emit("if");
        } else {
            emit("if (result " + resultType + ")");
        }
        indentLevel++;
    }

    public void emitElse() {
        indentLevel--;
        emit("else");
        indentLevel++;
    }

    public void emitIfEnd() {
        indentLevel--;
        emit("end");
    }

    /**
     * Emit block
     */
    public void emitBlockStart(String label) {
        emit("(block $" + label);
        indentLevel++;
    }

    public void emitBlockEnd() {
        indentLevel--;
        emit(")");
    }

    /**
     * Emit loop
     */
    public void emitLoopStart(String label) {
        emit("(loop $" + label);
        indentLevel++;
    }

    public void emitLoopEnd() {
        indentLevel--;
        emit(")");
    }

    /**
     * Emit branch
     */
    public void emitBranch(String label) {
        emit("br $" + label);
    }

    /**
     * Emit conditional branch
     */
    public void emitBranchIf(String label) {
        emit("br_if $" + label);
    }
}

