package com.languagei.compiler.codegen;

import java.util.*;

/**
 * Manages variable scoping for WebAssembly code generation.
 * Handles nested scopes and proper local variable indexing.
 */
public class VariableScopeManager {

    public static class VariableInfo {
        public final String name;
        public final String wasmType;
        public final int scopeLevel;
        public final int localIndex;

        public VariableInfo(String name, String wasmType, int scopeLevel, int localIndex) {
            this.name = name;
            this.wasmType = wasmType;
            this.scopeLevel = scopeLevel;
            this.localIndex = localIndex;
        }

        @Override
        public String toString() {
            return String.format("VariableInfo{name='%s', type='%s', scope=%d, index=%d}",
                               name, wasmType, scopeLevel, localIndex);
        }
    }

    // Stack of scopes, each scope contains a map of variable names to VariableInfo
    private final Deque<Map<String, VariableInfo>> scopes = new ArrayDeque<>();
    private int currentScopeLevel = 0;
    private int nextLocalIndex = 0;

    // Track all variables that need to be declared at function start
    private final List<VariableInfo> functionLocals = new ArrayList<>();

    public VariableScopeManager() {
        // Start with global scope (level 0)
        enterScope();
    }

    /**
     * Enter a new scope level
     */
    public void enterScope() {
        scopes.push(new HashMap<>());
        currentScopeLevel++;
    }

    /**
     * Exit current scope level
     */
    public void exitScope() {
        if (scopes.size() > 1) { // Don't exit the global scope
            scopes.pop();
            currentScopeLevel--;
        }
    }

    /**
     * Declare a new variable in the current scope
     */
    public VariableInfo declareVariable(String name, String wasmType) {
        Map<String, VariableInfo> currentScope = scopes.peek();
        if (currentScope.containsKey(name)) {
            throw new IllegalArgumentException("Variable '" + name + "' already declared in current scope");
        }

        VariableInfo varInfo = new VariableInfo(name, wasmType, currentScopeLevel, nextLocalIndex++);
        currentScope.put(name, varInfo);
        functionLocals.add(varInfo);

        return varInfo;
    }

    /**
     * Look up a variable by name, searching from inner to outer scopes
     */
    public VariableInfo lookupVariable(String name) {
        for (Map<String, VariableInfo> scope : scopes) {
            VariableInfo var = scope.get(name);
            if (var != null) {
                return var;
            }
        }
        return null;
    }

    /**
     * Get all local variables that need to be declared at function start
     */
    public List<VariableInfo> getFunctionLocals() {
        return new ArrayList<>(functionLocals);
    }

    /**
     * Reset for a new function
     */
    public void resetForNewFunction() {
        scopes.clear();
        functionLocals.clear();
        currentScopeLevel = 0;
        nextLocalIndex = 0;
        enterScope(); // Re-enter global scope
    }

    /**
     * Check if we're in global scope
     */
    public boolean isInGlobalScope() {
        return currentScopeLevel == 1;
    }

    /**
     * Get current scope level
     */
    public int getCurrentScopeLevel() {
        return currentScopeLevel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VariableScopeManager{\n");
        sb.append("  currentScopeLevel=").append(currentScopeLevel).append("\n");
        sb.append("  nextLocalIndex=").append(nextLocalIndex).append("\n");
        sb.append("  scopes=").append(scopes.size()).append("\n");

        int scopeIdx = 0;
        for (Map<String, VariableInfo> scope : scopes) {
            sb.append("    scope ").append(scopeIdx++).append(": ").append(scope.keySet()).append("\n");
        }

        sb.append("  functionLocals=[");
        for (int i = 0; i < functionLocals.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(functionLocals.get(i).name);
        }
        sb.append("]\n}");

        return sb.toString();
    }
}
