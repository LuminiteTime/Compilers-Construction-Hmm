package compiler.codegen;

/**
 * Bridge to C++ AST through JNI
 * This would be the interface to communicate with the C++ parser
 */
public class CppASTBridge {
    private long astPointer;  // Pointer to C++ AST
    private WasmCodeGenerator generator;

    /**
     * Create bridge with AST pointer from C++
     */
    public CppASTBridge(long astPointer, WasmCodeGenerator generator) {
        this.astPointer = astPointer;
        this.generator = generator;
    }

    /**
     * Load native library for JNI
     */
    static {
        try {
            System.loadLibrary("parser");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Could not load native parser library: " + e.getMessage());
            System.err.println("Code generation will work with stub data only");
        }
    }

    /**
     * Generate WASM code from C++ AST
     * This is a native method that would be implemented in C++
     */
    public native String generateWasmFromAST(long astPointer);

    /**
     * Get AST as JSON for debugging
     */
    public native String getASTAsJson(long astPointer);

    /**
     * Generate and write WASM to file
     */
    public void generateToFile(String filename) throws Exception {
        String wat = generateWasmFromAST(astPointer);
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get(filename),
            wat
        );
    }

    /**
     * Generate and return WASM
     */
    public String generate() {
        return generateWasmFromAST(astPointer);
    }

    /**
     * Get debug information
     */
    public String getDebugInfo() {
        return getASTAsJson(astPointer);
    }
}

