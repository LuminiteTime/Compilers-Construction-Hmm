package com.languagei.compiler.codegen;

import com.languagei.compiler.ast.RoutineDeclarationNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages function forward declarations and definitions
 */
public class FunctionEnvironment {

    private final Map<String, RoutineDeclarationNode> forwardDeclarations = new HashMap<>();
    private final Map<String, RoutineDeclarationNode> functionDefinitions = new HashMap<>();

    /**
     * Add a forward declaration
     */
    public void addForwardDeclaration(RoutineDeclarationNode function) {
        forwardDeclarations.put(function.getName(), function);
    }

    /**
     * Add a function definition
     */
    public void addFunctionDefinition(RoutineDeclarationNode function) {
        functionDefinitions.put(function.getName(), function);
    }

    /**
     * Check if a function has a forward declaration
     */
    public boolean hasForwardDeclaration(String functionName) {
        return forwardDeclarations.containsKey(functionName);
    }

    /**
     * Get forward declaration for a function
     */
    public RoutineDeclarationNode getForwardDeclaration(String functionName) {
        return forwardDeclarations.get(functionName);
    }

    /**
     * Get function definition
     */
    public RoutineDeclarationNode getFunctionDefinition(String functionName) {
        return functionDefinitions.get(functionName);
    }

    /**
     * Check if function is fully defined
     */
    public boolean isFunctionDefined(String functionName) {
        return functionDefinitions.containsKey(functionName) &&
               functionDefinitions.get(functionName).getBody() != null;
    }

    /**
     * Get all functions that need to be generated
     */
    public Map<String, RoutineDeclarationNode> getAllFunctions() {
        Map<String, RoutineDeclarationNode> allFunctions = new HashMap<>();

        // Add all definitions
        allFunctions.putAll(functionDefinitions);

        // Add forward declarations that don't have definitions
        for (Map.Entry<String, RoutineDeclarationNode> entry : forwardDeclarations.entrySet()) {
            if (!functionDefinitions.containsKey(entry.getKey())) {
                allFunctions.put(entry.getKey(), entry.getValue());
            }
        }

        return allFunctions;
    }

    /**
     * Clear all function declarations
     */
    public void clear() {
        forwardDeclarations.clear();
        functionDefinitions.clear();
    }
}
