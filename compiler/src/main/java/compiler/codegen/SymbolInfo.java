package compiler.codegen;

/**
 * Information about a symbol for code generation
 */
public class SymbolInfo {
    private String name;
    private String type;
    private SymbolKind kind;
    private int wasmIndex;  // Local/global/function index in WASM
    private int memoryOffset;  // For record fields
    private boolean isParameter;

    public enum SymbolKind {
        LOCAL, GLOBAL, PARAMETER, FUNCTION, TYPE
    }

    public SymbolInfo(String name, String type, SymbolKind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.wasmIndex = -1;
        this.memoryOffset = 0;
        this.isParameter = false;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public int getWasmIndex() {
        return wasmIndex;
    }

    public void setWasmIndex(int index) {
        this.wasmIndex = index;
    }

    public int getMemoryOffset() {
        return memoryOffset;
    }

    public void setMemoryOffset(int offset) {
        this.memoryOffset = offset;
    }

    public boolean isParameter() {
        return isParameter;
    }

    public void setParameter(boolean parameter) {
        isParameter = parameter;
    }

    @Override
    public String toString() {
        return "SymbolInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", kind=" + kind +
                ", wasmIndex=" + wasmIndex +
                ", memoryOffset=" + memoryOffset +
                '}';
    }
}

