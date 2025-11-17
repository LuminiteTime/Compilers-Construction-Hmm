package compiler.codegen;

/**
 * WebAssembly primitive types
 */
public enum WasmType {
    I32("i32"),
    I64("i64"),
    F32("f32"),
    F64("f64");

    private final String wasmName;

    WasmType(String wasmName) {
        this.wasmName = wasmName;
    }

    public String getWasmName() {
        return wasmName;
    }

    /**
     * Get WASM type for language type
     */
    public static WasmType fromLanguageType(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer" -> I32;
            case "real" -> F64;
            case "boolean" -> I32;  // 0 = false, 1 = true
            case "array", "record" -> I32;  // pointer to memory
            default -> throw new CodeGenException("Unknown language type: " + langType);
        };
    }
}

