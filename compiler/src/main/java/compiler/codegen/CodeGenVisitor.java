package compiler.codegen;

import java.util.Map;

/**
 * Code generation visitor implementation
 * Traverses AST and emits WASM code
 */
public class CodeGenVisitor implements ASTVisitor {
    private WasmCodeGenerator generator;
    private String currentFunctionName;
    private String currentFunctionReturnType;

    public CodeGenVisitor(WasmCodeGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void visitProgram(Object programNode) {
        if (!(programNode instanceof Map)) return;

        Map<String, Object> program = (Map<String, Object>) programNode;

        // Process declarations first
        Object declarations = program.get("declarations");
        if (declarations instanceof java.util.List) {
            for (Object decl : (java.util.List<?>) declarations) {
                if (decl instanceof Map) {
                    Map<String, Object> declMap = (Map<String, Object>) decl;
                    String type = (String) declMap.get("type");
                    if ("variable".equals(type)) {
                        visitVariableDeclaration(declMap);
                    } else if ("routine".equals(type)) {
                        visitRoutineDeclaration(declMap);
                    } else if ("type".equals(type)) {
                        visitTypeDeclaration(declMap);
                    }
                }
            }
        }

        // Process statements
        Object statements = program.get("statements");
        if (statements instanceof java.util.List) {
            for (Object stmt : (java.util.List<?>) statements) {
                if (stmt instanceof Map) {
                    Map<String, Object> stmtMap = (Map<String, Object>) stmt;
                    visitStatement(stmtMap);
                }
            }
        }
    }

    private void visitStatement(Map<String, Object> stmtMap) {
        String type = (String) stmtMap.get("type");
        switch (type) {
            case "assignment" -> visitAssignment(stmtMap);
            case "print" -> visitPrintStatement(stmtMap);
            case "while" -> visitWhileLoop(stmtMap);
            case "for" -> visitForLoop(stmtMap);
            case "if" -> visitIfStatement(stmtMap);
            case "return" -> visitReturnStatement(stmtMap);
            case "routine_call" -> visitRoutineCall(stmtMap);
        }
    }

    @Override
    public void visitVariableDeclaration(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> decl = (Map<String, Object>) node;
        String name = (String) decl.get("name");
        String varType = (String) decl.get("varType");
        Object typeInfo = decl.get("type");
        Object initializer = decl.get("initializer");

        if (varType == null) varType = "integer"; // default

        // Handle complex types (arrays, records)
        if (typeInfo instanceof Map) {
            Map<String, Object> typeMap = (Map<String, Object>) typeInfo;
            String typeKind = (String) typeMap.get("type");

            if ("array".equals(typeKind)) {
                handleArrayDeclaration(name, typeMap, initializer);
            } else if ("record".equals(typeKind)) {
                handleRecordDeclaration(name, typeMap, initializer);
            } else {
                // Simple type
                handleSimpleDeclaration(name, varType, initializer);
            }
        } else {
            // Simple type
            handleSimpleDeclaration(name, varType, initializer);
        }
    }

    private void handleSimpleDeclaration(String name, String varType, Object initializer) {
        // For global variables, emit global declaration
        if (currentFunctionName == null) { // global scope
            String initialValue = getInitialValue(varType, initializer);
            generator.emitGlobalDeclaration(name, varType, initialValue);
        } else {
            // For local variables, declare in current function
            generator.emitLocalDeclaration(name, varType);

            // If there's an initializer, emit assignment
            if (initializer != null) {
                visitExpression(initializer);
                generator.emitStore(name);
            }
        }
    }

    private void handleArrayDeclaration(String name, Map<String, Object> typeMap, Object initializer) {
        // Arrays are pointers to memory, so they're i32 in WASM
        Object size = typeMap.get("size");
        Object elementType = typeMap.get("elementType");

        if (currentFunctionName == null) { // global scope
            // For global arrays, allocate memory at startup
            // For now, just store a pointer (will be initialized to 0)
            generator.emitGlobalDeclaration(name, "integer", "0");
        } else {
            // For local arrays, declare as local pointer
            generator.emitLocalDeclaration(name, "integer");

            // Allocate memory for the array
            if (size instanceof Map) {
                Map<String, Object> sizeMap = (Map<String, Object>) size;
                if ("integer_literal".equals(sizeMap.get("type"))) {
                    String sizeValue = (String) sizeMap.get("value");
                    int arraySize = Integer.parseInt(sizeValue);
                    int elementSize = 4; // Assume 4 bytes per element for now
                    int totalSize = arraySize * elementSize + 4; // +4 for size field

                    // Allocate memory
                    generator.emit("i32.const " + totalSize);
                    generator.emit("call $alloc");
                    generator.emitStore(name);

                    // Store array size at the beginning
                    generator.emitLoad(name);
                    generator.emit("i32.const " + arraySize);
                    generator.emit("i32.store");
                }
            }

            // TODO: Handle array initialization
        }
    }

    private void handleRecordDeclaration(String name, Map<String, Object> typeMap, Object initializer) {
        // Records are pointers to memory, so they're i32 in WASM
        Object body = typeMap.get("body");

        if (currentFunctionName == null) { // global scope
            // For global records, allocate memory at startup
            generator.emitGlobalDeclaration(name, "integer", "0");
        } else {
            // For local records, declare as local pointer
            generator.emitLocalDeclaration(name, "integer");

            // Calculate record size and allocate memory
            int recordSize = calculateRecordSize(body);
            if (recordSize > 0) {
                generator.emit("i32.const " + recordSize);
                generator.emit("call $alloc");
                generator.emitStore(name);
            }

            // TODO: Handle record initialization
        }
    }

    private int calculateRecordSize(Object body) {
        if (!(body instanceof java.util.List)) return 0;

        int size = 0;
        for (Object field : (java.util.List<?>) body) {
            if (field instanceof Map) {
                Map<String, Object> fieldMap = (Map<String, Object>) field;
                String fieldType = (String) fieldMap.get("varType");
                if (fieldType != null) {
                    size += generator.getTypeSize(fieldType);
                } else {
                    size += 4; // default size
                }
            }
        }
        return size;
    }

    private String getInitialValue(String varType, Object initializer) {
        if (initializer instanceof Map) {
            Map<String, Object> initMap = (Map<String, Object>) initializer;
            String initType = (String) initMap.get("type");
            if ("integer_literal".equals(initType)) {
                return (String) initMap.get("value");
            } else if ("real_literal".equals(initType)) {
                return (String) initMap.get("value");
            } else if ("boolean_literal".equals(initType)) {
                return "true".equals(initMap.get("value")) ? "1" : "0";
            }
        }
        // Default values
        return switch (varType.toLowerCase()) {
            case "integer" -> "0";
            case "real" -> "0.0";
            case "boolean" -> "0";
            default -> "0";
        };
    }

    @Override
    public void visitTypeDeclaration(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> typeDecl = (Map<String, Object>) node;
        String name = (String) typeDecl.get("name");
        Object type = typeDecl.get("type");

        // Register the type in the symbol table for later use
        if (type instanceof Map) {
            Map<String, Object> typeMap = (Map<String, Object>) type;
            String typeKind = (String) typeMap.get("type");
            if ("record".equals(typeKind) || "array".equals(typeKind)) {
                // Store type information for later use in variable declarations
                generator.getSymbolTable().declareFunction(name, "type"); // Use function slot for type info
            }
        }
    }

    @Override
    public void visitRoutineDeclaration(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> routine = (Map<String, Object>) node;
        Object header = routine.get("header");
        Object body = routine.get("body");

        if (header instanceof Map) {
            visitRoutineHeader(header);
        }

        // Generate function body
        if (body instanceof Map) {
            visitRoutineBody(body);
        }

        // Reset function context
        currentFunctionName = null;
        currentFunctionReturnType = null;
        generator.getSymbolTable().resetLocalIndices();
    }

    @Override
    public void visitRoutineHeader(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> header = (Map<String, Object>) node;
        String name = (String) header.get("name");
        String returnType = (String) header.get("returnType");
        Object parameters = header.get("parameters");

        currentFunctionName = name;
        currentFunctionReturnType = returnType != null ? returnType : "void";

        // Declare function in symbol table
        generator.getSymbolTable().declareFunction(name, currentFunctionReturnType);

        // Start function definition
        StringBuilder funcDecl = new StringBuilder();
        funcDecl.append("(func $").append(name);

        // Process parameters
        if (parameters instanceof java.util.List) {
            for (Object param : (java.util.List<?>) parameters) {
                if (param instanceof Map) {
                    Map<String, Object> paramMap = (Map<String, Object>) param;
                    String paramName = (String) paramMap.get("name");
                    String paramType = (String) paramMap.get("type");
                    generator.getSymbolTable().declareParameter(paramName, paramType);
                    funcDecl.append(" (param $").append(paramName).append(" ")
                            .append(generator.languageTypeToWasm(paramType)).append(")");
                }
            }
        }

        // Add return type if not void
        if (!"void".equals(currentFunctionReturnType)) {
            funcDecl.append(" (result ").append(generator.languageTypeToWasm(currentFunctionReturnType)).append(")");
        }

        generator.emit(funcDecl.toString());
        generator.getSymbolTable().enterScope();
    }

    @Override
    public void visitRoutineBody(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> body = (Map<String, Object>) node;
        Object statements = body.get("statements");

        // Process statements
        if (statements instanceof java.util.List) {
            for (Object stmt : (java.util.List<?>) statements) {
                if (stmt instanceof Map) {
                    visitStatement((Map<String, Object>) stmt);
                }
            }
        }

        generator.emit(")"); // Close function
        generator.getSymbolTable().exitScope();
    }

    @Override
    public void visitAssignment(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> assignment = (Map<String, Object>) node;
        Object target = assignment.get("target");
        Object value = assignment.get("value");

        // Evaluate value first
        visitExpression(value);

        // Store to target
        if (target instanceof Map) {
            Map<String, Object> targetMap = (Map<String, Object>) target;
            String targetType = (String) targetMap.get("type");
            if ("variable_access".equals(targetType)) {
                String varName = (String) targetMap.get("name");
                generator.emitStore(varName);
            }
            // TODO: Handle array access and field access targets
        }
    }

    @Override
    public void visitPrintStatement(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> printStmt = (Map<String, Object>) node;
        Object expressions = printStmt.get("expressions");

        if (expressions instanceof java.util.List) {
            for (Object expr : (java.util.List<?>) expressions) {
                visitExpression(expr);
                // Assume integer for now, call print_int
                generator.emitFunctionCall("$print_int");
            }
        }
    }

    @Override
    public void visitWhileLoop(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> whileLoop = (Map<String, Object>) node;
        Object condition = whileLoop.get("condition");
        Object body = whileLoop.get("body");

        String breakLabel = generator.getSymbolTable().getNextFunctionIndex() + "_break";
        String continueLabel = generator.getSymbolTable().getNextFunctionIndex() + "_continue";

        generator.emitBlockStart(breakLabel);
        generator.emitLoopStart(continueLabel);

        // Condition (negated for break)
        visitExpression(condition);
        generator.emit("i32.eqz"); // negate condition
        generator.emitBranch(breakLabel);

        // Body
        if (body instanceof java.util.List) {
            for (Object stmt : (java.util.List<?>) body) {
                if (stmt instanceof Map) {
                    visitStatement((Map<String, Object>) stmt);
                }
            }
        }

        generator.emitBranch(continueLabel);
        generator.emitLoopEnd();
        generator.emitBlockEnd();
    }

    @Override
    public void visitForLoop(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> forLoop = (Map<String, Object>) node;
        String loopVar = (String) forLoop.get("loopVar");
        Object range = forLoop.get("range");
        Boolean reverse = (Boolean) forLoop.get("reverse");
        Object body = forLoop.get("body");

        if (reverse == null) reverse = false;

        // Declare loop variable
        generator.emitLocalDeclaration(loopVar, "integer");

        if (range instanceof Map) {
            Map<String, Object> rangeMap = (Map<String, Object>) range;
            Object start = rangeMap.get("start");
            Object end = rangeMap.get("end");

            // Initialize loop variable
            visitExpression(start);
            generator.emitStore(loopVar);

            String breakLabel = generator.getSymbolTable().getNextFunctionIndex() + "_break";
            String continueLabel = generator.getSymbolTable().getNextFunctionIndex() + "_continue";

            generator.emitBlockStart(breakLabel);
            generator.emitLoopStart(continueLabel);

            // Condition: loopVar <= end (or >= start for reverse)
            generator.emitLoad(loopVar);
            visitExpression(end);
            if (reverse) {
                generator.emit("i32.ge_s");
            } else {
                generator.emit("i32.le_s");
            }
            generator.emit("i32.eqz");
            generator.emitBranch(breakLabel);

            // Body
            if (body instanceof java.util.List) {
                for (Object stmt : (java.util.List<?>) body) {
                    if (stmt instanceof Map) {
                        visitStatement((Map<String, Object>) stmt);
                    }
                }
            }

            // Increment/decrement
            generator.emitLoad(loopVar);
            generator.emit("i32.const " + (reverse ? "-1" : "1"));
            generator.emit("i32.add");
            generator.emitStore(loopVar);

            generator.emitBranch(continueLabel);
            generator.emitLoopEnd();
            generator.emitBlockEnd();
        }
    }

    @Override
    public void visitIfStatement(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> ifStmt = (Map<String, Object>) node;
        Object condition = ifStmt.get("condition");
        Object thenBody = ifStmt.get("thenBody");
        Object elseBody = ifStmt.get("elseBody");

        // Start if block
        generator.emitIfStart(null);

        // Condition
        visitExpression(condition);

        // Then body
        if (thenBody instanceof java.util.List) {
            for (Object stmt : (java.util.List<?>) thenBody) {
                if (stmt instanceof Map) {
                    visitStatement((Map<String, Object>) stmt);
                }
            }
        }

        // Else body
        if (elseBody != null && elseBody instanceof java.util.List && !((java.util.List<?>) elseBody).isEmpty()) {
            generator.emitElse();
            for (Object stmt : (java.util.List<?>) elseBody) {
                if (stmt instanceof Map) {
                    visitStatement((Map<String, Object>) stmt);
                }
            }
        }

        generator.emitIfEnd();
    }

    @Override
    public void visitReturnStatement(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> returnStmt = (Map<String, Object>) node;
        Object expression = returnStmt.get("expression");

        if (expression != null) {
            visitExpression(expression);
        }

        generator.emit("return");
    }

    @Override
    public void visitExpression(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> expr = (Map<String, Object>) node;
        String type = (String) expr.get("type");

        switch (type) {
            case "integer_literal" -> visitIntegerLiteral(expr);
            case "real_literal" -> visitRealLiteral(expr);
            case "boolean_literal" -> visitBooleanLiteral(expr);
            case "string_literal" -> visitStringLiteral(expr);
            case "variable_access" -> visitVariableAccess(expr);
            case "binary_op" -> visitBinaryOp(expr);
            case "unary_op" -> visitUnaryOp(expr);
            case "array_access" -> visitArrayAccess(expr);
            case "field_access" -> visitFieldAccess(expr);
            case "routine_call" -> visitRoutineCall(expr);
        }
    }

    @Override
    public void visitBinaryOp(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> binOp = (Map<String, Object>) node;
        String op = (String) binOp.get("op");
        Object left = binOp.get("left");
        Object right = binOp.get("right");
        String operandType = (String) binOp.get("operandType");

        // Evaluate operands
        visitExpression(left);
        visitExpression(right);

        // Emit operation
        generator.emitBinaryOp(op, operandType);
    }

    @Override
    public void visitUnaryOp(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> unaryOp = (Map<String, Object>) node;
        String op = (String) unaryOp.get("op");
        Object operand = unaryOp.get("operand");
        String operandType = (String) unaryOp.get("operandType");

        // Evaluate operand
        visitExpression(operand);

        // Emit operation
        generator.emitUnaryOp(op, operandType);
    }

    @Override
    public void visitIntegerLiteral(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> intLit = (Map<String, Object>) node;
        String value = (String) intLit.get("value");
        generator.emitConstant("integer", value);
    }

    @Override
    public void visitRealLiteral(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> realLit = (Map<String, Object>) node;
        String value = (String) realLit.get("value");
        generator.emitConstant("real", value);
    }

    @Override
    public void visitBooleanLiteral(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> boolLit = (Map<String, Object>) node;
        String value = (String) boolLit.get("value");
        generator.emitConstant("boolean", "true".equals(value) ? "1" : "0");
    }

    @Override
    public void visitStringLiteral(Object node) {
        // String literals not fully supported yet
        // For now, just emit empty string or handle as integer 0
        generator.emitConstant("integer", "0");
    }

    @Override
    public void visitVariableAccess(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> varAccess = (Map<String, Object>) node;
        String name = (String) varAccess.get("name");
        generator.emitLoad(name);
    }

    @Override
    public void visitArrayAccess(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> arrayAccess = (Map<String, Object>) node;
        Object array = arrayAccess.get("array");
        Object index = arrayAccess.get("index");

        // Load array base pointer
        visitExpression(array);

        // Calculate address: base + (index-1)*element_size + 4 (skip size field)
        visitExpression(index);
        generator.emit("i32.const 1");
        generator.emit("i32.sub"); // 1-based to 0-based indexing
        generator.emit("i32.const 4"); // element size
        generator.emit("i32.mul");
        generator.emit("i32.const 4"); // skip size field
        generator.emit("i32.add");
        generator.emit("i32.add");

        // Load value from memory
        generator.emit("i32.load");
    }

    @Override
    public void visitFieldAccess(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> fieldAccess = (Map<String, Object>) node;
        Object record = fieldAccess.get("record");
        String fieldName = (String) fieldAccess.get("fieldName");

        // Load record base pointer
        visitExpression(record);

        // Add field offset (simplified - assume first field at offset 0)
        // TODO: Look up actual field offset from symbol table
        generator.emit("i32.const 0"); // field offset
        generator.emit("i32.add");

        // Load field value
        generator.emit("i32.load");
    }

    @Override
    public void visitRoutineCall(Object node) {
        if (!(node instanceof Map)) return;

        Map<String, Object> routineCall = (Map<String, Object>) node;
        String name = (String) routineCall.get("name");
        Object arguments = routineCall.get("arguments");

        // Evaluate arguments
        if (arguments instanceof java.util.List) {
            for (Object arg : (java.util.List<?>) arguments) {
                visitExpression(arg);
            }
        }

        // Call function
        generator.emitFunctionCall(name);
    }

    /**
     * Get the generator for direct emission
     */
    public WasmCodeGenerator getGenerator() {
        return generator;
    }

    /**
     * Set current function context
     */
    public void setCurrentFunction(String name, String returnType) {
        this.currentFunctionName = name;
        this.currentFunctionReturnType = returnType;
    }

    /**
     * Get current function name
     */
    public String getCurrentFunctionName() {
        return currentFunctionName;
    }

    /**
     * Get current function return type
     */
    public String getCurrentFunctionReturnType() {
        return currentFunctionReturnType;
    }
}