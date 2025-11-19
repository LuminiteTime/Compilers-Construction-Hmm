package compiler.codegen;

import java.util.*;

/**
 * Symbol table for code generation with WASM-specific information
 */
public class CodeGenSymbolTable {
    private final Stack<Map<String, SymbolInfo>> scopes;
    private final Map<String, SymbolInfo> globalFunctions;
    private int nextLocalIndex;
    private int nextGlobalIndex;
    private int nextFunctionIndex;
    private int heapPointer = 0x1000;  // Start of heap

    public CodeGenSymbolTable() {
        scopes = new Stack<>();
        globalFunctions = new HashMap<>();
        enterScope();
        nextLocalIndex = 0;
        nextGlobalIndex = 0;
        nextFunctionIndex = 0;
    }

    /**
     * Enter a new scope
     */
    public void enterScope() {
        scopes.push(new HashMap<>());
        nextLocalIndex = 0;
    }

    /**
     * Exit current scope
     */
    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    /**
     * Declare a local variable
     */
    public void declareLocal(String name, String type) {
        SymbolInfo info = new SymbolInfo(name, type, SymbolInfo.SymbolKind.LOCAL);
        info.setWasmIndex(nextLocalIndex++);
        scopes.peek().put(name, info);
    }

    /**
     * Declare a global variable
     */
    public void declareGlobal(String name, String type) {
        SymbolInfo info = new SymbolInfo(name, type, SymbolInfo.SymbolKind.GLOBAL);
        info.setWasmIndex(nextGlobalIndex++);
        scopes.peek().put(name, info);
    }

    /**
     * Declare a parameter
     */
    public void declareParameter(String name, String type) {
        SymbolInfo info = new SymbolInfo(name, type, SymbolInfo.SymbolKind.PARAMETER);
        info.setWasmIndex(nextLocalIndex++);
        info.setParameter(true);
        scopes.peek().put(name, info);
    }

    /**
     * Declare a function
     */
    public void declareFunction(String name, String returnType) {
        SymbolInfo info = new SymbolInfo(name, returnType, SymbolInfo.SymbolKind.FUNCTION);
        info.setWasmIndex(nextFunctionIndex++);
        globalFunctions.put(name, info);
    }

    /**
     * Lookup symbol in current scope chain
     */
    public SymbolInfo lookup(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolInfo info = scopes.get(i).get(name);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    /**
     * Lookup function (global scope)
     */
    public SymbolInfo lookupFunction(String name) {
        return globalFunctions.get(name);
    }

    /**
     * Check if symbol exists in current scope
     */
    public boolean existsInCurrentScope(String name) {
        return scopes.peek().containsKey(name);
    }

    /**
     * Get all symbols in current scope
     */
    public Collection<SymbolInfo> getCurrentScopeSymbols() {
        return scopes.peek().values();
    }

    /**
     * Allocate memory for data structure
     */
    public int allocateMemory(int size) {
        int ptr = heapPointer;
        heapPointer += size;
        return ptr;
    }

    /**
     * Get next local index and increment
     */
    public int getNextLocalIndex() {
        return nextLocalIndex++;
    }

    /**
     * Get next global index and increment
     */
    public int getNextGlobalIndex() {
        return nextGlobalIndex++;
    }

    /**
     * Get next function index and increment
     */
    public int getNextFunctionIndex() {
        return nextFunctionIndex++;
    }

    /**
     * Get current heap pointer
     */
    public int getHeapPointer() {
        return heapPointer;
    }

    /**
     * Get all local variables in current scope
     */
    public Map<String, SymbolInfo> getLocalVariables() {
        return new HashMap<>(scopes.peek());
    }

    /**
     * Reset for function generation
     */
    public void resetLocalIndices() {
        nextLocalIndex = 0;
    }
}

