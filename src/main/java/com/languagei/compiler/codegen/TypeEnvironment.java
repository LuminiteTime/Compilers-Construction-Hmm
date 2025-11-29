package com.languagei.compiler.codegen;

import com.languagei.compiler.ast.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages type aliases and type resolution
 */
public class TypeEnvironment {

    private final Map<String, ASTNode> typeAliases = new HashMap<>();

    /**
     * Add a type alias
     */
    public void addTypeAlias(String name, ASTNode typeDefinition) {
        typeAliases.put(name, typeDefinition);
    }

    /**
     * Resolve a type reference by following aliases
     */
    public ASTNode resolveType(String typeName) {
        return typeAliases.get(typeName);
    }

    /**
     * Check if a type name is an alias
     */
    public boolean isTypeAlias(String name) {
        return typeAliases.containsKey(name);
    }

    /**
     * Get all type aliases
     */
    public Map<String, ASTNode> getAllAliases() {
        return new HashMap<>(typeAliases);
    }

    /**
     * Clear all type aliases
     */
    public void clear() {
        typeAliases.clear();
    }
}
