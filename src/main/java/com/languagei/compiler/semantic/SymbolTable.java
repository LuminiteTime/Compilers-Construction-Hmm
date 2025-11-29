package com.languagei.compiler.semantic;

import java.util.*;

/**
 * Hierarchical symbol table for scoping
 */
public class SymbolTable {
    private final Deque<Map<String, Symbol>> scopes;

    public SymbolTable() {
        this.scopes = new LinkedList<>();
        // Global scope
        scopes.push(new HashMap<>());
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
        }
    }

    public void declare(String name, Symbol symbol) {
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope == null) {
            throw new RuntimeException("No scope available");
        }
        currentScope.put(name, symbol);
    }

    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    public Symbol lookupLocal(String name) {
        Map<String, Symbol> currentScope = scopes.peek();
        if (currentScope != null) {
            return currentScope.get(name);
        }
        return null;
    }

    public boolean isDeclaredInCurrentScope(String name) {
        Map<String, Symbol> currentScope = scopes.peek();
        return currentScope != null && currentScope.containsKey(name);
    }

    public int getCurrentScopeLevel() {
        return scopes.size();
    }

    public void clear() {
        scopes.clear();
        scopes.push(new HashMap<>());
    }
}

