package compiler.codegen;

import java.util.ArrayList;
import java.util.List;

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
    public void emitAllocFunction() {
        watOutput.append("  (func $alloc (param $size i32) (result i32)\n");
        watOutput.append("    (local $ptr i32)\n");
        watOutput.append("    (local $aligned_ptr i32)\n");
        watOutput.append("    (global.get $heap_ptr)\n");
        watOutput.append("    local.set $ptr\n");
        watOutput.append("    local.get $ptr\n");
        watOutput.append("    i32.const 3\n");
        watOutput.append("    i32.add\n");
        watOutput.append("    i32.const 4\n");
        watOutput.append("    i32.const -1\n");
        watOutput.append("    i32.xor\n");
        watOutput.append("    i32.and\n"); // align to 4 bytes
        watOutput.append("    local.set $aligned_ptr\n");
        watOutput.append("    local.get $aligned_ptr\n");
        watOutput.append("    local.get $size\n");
        watOutput.append("    i32.add\n");
        watOutput.append("    (global.set $heap_ptr)\n");
        watOutput.append("    local.get $aligned_ptr\n");
        watOutput.append("  )\n");
    }

    /**
     * Emit print functions with fixed buffers
     */
    public void emitPrintFunctionsFixed() {
        // Save current indent level
        int savedIndent = indentLevel;
        indentLevel = 0;

        // Simple print_int function that only handles small numbers
        watOutput.append("(func $print_int (param $value i32)\n");
        watOutput.append("  local.get $value\n");
        watOutput.append("  i32.const 48\n");
        watOutput.append("  i32.add\n");
        watOutput.append("  call $print_char\n");
        watOutput.append(")\n");

        // print_char function
        watOutput.append("(func $print_char (param $char i32)\n");
        watOutput.append("  (global.get $print_buffer)\n"); // get allocated buffer
        watOutput.append("  local.get $char\n");
        watOutput.append("  i32.store8\n"); // store char in buffer
        watOutput.append("  (global.get $iovec_buffer)\n"); // iovec base address
        watOutput.append("  (global.get $print_buffer)\n"); // data pointer
        watOutput.append("  i32.store\n"); // store iov_base
        watOutput.append("  (global.get $iovec_buffer)\n");
        watOutput.append("  i32.const 4\n");
        watOutput.append("  i32.add\n"); // iovec len address
        watOutput.append("  i32.const 1\n"); // length = 1
        watOutput.append("  i32.store\n"); // store iov_len
        watOutput.append("  i32.const 1\n"); // stdout
        watOutput.append("  (global.get $iovec_buffer)\n"); // iovecs pointer
        watOutput.append("  i32.const 1\n"); // iovecs_len
        watOutput.append("  i32.const 0\n"); // nwritten (ignored)
        watOutput.append("  call $fd_write\n");
        watOutput.append("  drop\n"); // ignore result
        watOutput.append(")\n");

        // No-op init function for fixed buffers
        watOutput.append("(func $init_print_buffer\n");
        watOutput.append("  ;; Buffers are pre-allocated with fixed addresses\n");
        watOutput.append(")\n");

        // Restore indent level
        indentLevel = savedIndent;
    }

    /**
     * Emit print functions
     */
    public void emitPrintFunctions() {
        // Save current indent level
        int savedIndent = indentLevel;
        indentLevel = 0;

        // Simple print_int function that only handles small numbers
        watOutput.append("(func $print_int (param $value i32)\n");
        watOutput.append("  local.get $value\n");
        watOutput.append("  i32.const 48\n");
        watOutput.append("  i32.add\n");
        watOutput.append("  call $print_char\n");
        watOutput.append(")\n");

        // print_char function
        watOutput.append("(func $print_char (param $char i32)\n");
        watOutput.append("  (global.get $print_buffer)\n"); // get allocated buffer
        watOutput.append("  local.get $char\n");
        watOutput.append("  i32.store8\n"); // store char in buffer
        watOutput.append("  (global.get $iovec_buffer)\n"); // iovec base address
        watOutput.append("  (global.get $print_buffer)\n"); // data pointer
        watOutput.append("  i32.store\n"); // store iov_base
        watOutput.append("  (global.get $iovec_buffer)\n");
        watOutput.append("  i32.const 4\n");
        watOutput.append("  i32.add\n"); // iovec len address
        watOutput.append("  i32.const 1\n"); // length = 1
        watOutput.append("  i32.store\n"); // store iov_len
        watOutput.append("  i32.const 1\n"); // stdout
        watOutput.append("  (global.get $iovec_buffer)\n"); // iovecs pointer
        watOutput.append("  i32.const 1\n"); // iovecs_len
        watOutput.append("  i32.const 0\n"); // nwritten (ignored)
        watOutput.append("  call $fd_write\n");
        watOutput.append("  drop\n"); // ignore result
        watOutput.append(")\n");

        // Print buffer globals
        watOutput.append("(global $print_buffer (mut i32) (i32.const 0))\n");
        watOutput.append("(global $print_buffer_size (mut i32) (i32.const 32))\n");
        watOutput.append("(global $iovec_buffer (mut i32) (i32.const 0))\n");

        // Init print buffer function
        watOutput.append("(func $init_print_buffer\n");
        watOutput.append("  (global.get $print_buffer)\n");
        watOutput.append("  i32.eqz\n");
        watOutput.append("  if\n");
        watOutput.append("    (global.get $print_buffer_size)\n");
        watOutput.append("    call $alloc\n");
        watOutput.append("    (global.set $print_buffer)\n");
        watOutput.append("  end\n");
        watOutput.append("  (global.get $iovec_buffer)\n");
        watOutput.append("  i32.eqz\n");
        watOutput.append("  if\n");
        watOutput.append("    i32.const 8\n"); // space for iovec
        watOutput.append("    call $alloc\n");
        watOutput.append("    (global.set $iovec_buffer)\n");
        watOutput.append("  end\n");
        watOutput.append(")\n");

        // Restore indent level
        indentLevel = savedIndent;
    }

    /**
     * Helper: emit line with indentation
     */
    public void emit(String code) {
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

    /**
     * Emit WASI imports
     */
    public void emitWasiImports() {
        // Only add if not already added
        String fdWrite = "(import \"wasi_snapshot_preview1\" \"fd_write\"\n" +
                "  (func $fd_write (param i32 i32 i32 i32) (result i32)))";
        String procExit = "(import \"wasi_snapshot_preview1\" \"proc_exit\"\n" +
                "  (func $proc_exit (param i32)))";

        if (!imports.contains(fdWrite)) {
            imports.add(fdWrite);
        }
        if (!imports.contains(procExit)) {
            imports.add(procExit);
        }
    }

    /**
     * Emit memory declaration
     */
    public void emitMemory() {
        emit("(memory 1)");
        emit("(export \"memory\" (memory 0))");
    }

    /**
     * Emit heap pointer global
     */
    public void emitHeapPtr() {
        emit("(global $heap_ptr (mut i32) (i32.const 0x1000))");
    }

    /**
     * Emit start function
     */
    public void emitStartFunction() {
        emit("(func $_start");
        emit("  call $init_print_buffer");
        emit("  ;; Program statements will be emitted here");
        emit(")");
        emit("(export \"_start\" (func $_start))");
    }

    /**
     * Get the generated WAT output
     */
    public String getOutput() {
        return watOutput.toString();
    }

}

