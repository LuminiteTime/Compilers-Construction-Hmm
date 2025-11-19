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
            // Try to load from the expected location
            String libPath = System.getProperty("user.dir") + "/compiler/src/main/cpp/parser/libparser.so";
            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Could not load native parser library from " +
                             System.getProperty("user.dir") + "/compiler/src/main/cpp/parser/libparser.so: " + e.getMessage());
            System.err.println("Code generation will work with stub data only");
        }
    }

    /**
     * Get the AST pointer from the last parse operation
     */
    public native long getASTPointer();

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
     * Generate and return WASM using Java code generator with AST traversal
     */
    public String generate() {
        System.out.println("DEBUG: CppASTBridge.generate() called - using Java codegen");
        try {
            WasmCodeGenerator generator = new WasmCodeGenerator();
            CodeGenVisitor visitor = new CodeGenVisitor(generator);

            // Get AST as JSON from C++ and parse it
            String astJson = getASTAsJson(astPointer);
            if (astJson == null || astJson.trim().isEmpty()) {
                // Fallback to minimal module
                return createMinimalModule();
            }

            // Parse JSON and generate real WASM code
            return generateFromJson(astJson, generator, visitor);

        } catch (Exception e) {
            throw new RuntimeException("Code generation failed", e);
        }
    }

    /**
     * Generate WASM from JSON AST representation
     */
    private String generateFromJson(String astJson, WasmCodeGenerator generator, CodeGenVisitor visitor) {
        try {
            System.out.println("DEBUG: Processing AST JSON for code generation");

            // Parse JSON using simple parser (since Gson not available)
            var json = parseSimpleJson(astJson);
            if (json == null) {
                return createMinimalWorkingModule();
            }

            // Initialize generator with basic setup - start module
            generator.emit("(module");
            generator.emitWasiImports();
            generator.emitMemory();
            generator.emitHeapPtr();
            generator.emitAllocFunction();
            generator.emitPrintFunctions();

            // First pass: collect local variables by visiting declarations
            visitor.visitProgram(json);

            // Generate _start function with local variable declarations
            generator.emit("(func $_start");

            // Emit local variable declarations from symbol table
            var localVars = generator.getSymbolTable().getLocalVariables();
            for (var entry : localVars.entrySet()) {
                String name = entry.getKey();
                SymbolInfo info = entry.getValue();
                String wasmType = generator.languageTypeToWasm(info.getType());
                generator.emit("  (local $" + name + " " + wasmType + ")");
            }

            generator.emit("  call $init_print_buffer");

            // Second pass: generate code for statements
            var statements = json.get("statements");
            if (statements instanceof java.util.List<?> stmtList) {
                for (var stmt : stmtList) {
                    if (stmt instanceof java.util.Map<?,?> stmtMap) {
                        processStatementFromMap(stmtMap, generator);
                    }
                }
            }

            generator.emit(")");
            generator.emit("(export \"_start\" (func $_start))");
            generator.emit(")"); // Close module

            return generator.getOutput();

        } catch (Exception e) {
            System.err.println("Error processing AST JSON: " + e.getMessage());
            e.printStackTrace();
            return createMinimalWorkingModule();
        }
    }

    /**
     * Simple JSON parser for AST (basic implementation)
     */
    private java.util.Map<String, Object> parseSimpleJson(String json) {
        try {
            // Very basic JSON parser for our specific format
            var result = new java.util.HashMap<String, Object>();

            // Extract declarations array
            var declStart = json.indexOf("\"declarations\":");
            if (declStart >= 0) {
                var bracketStart = json.indexOf('[', declStart);
                var bracketEnd = findMatchingBracket(json, bracketStart);
                if (bracketStart >= 0 && bracketEnd > bracketStart) {
                    var declarationsJson = json.substring(bracketStart + 1, bracketEnd);
                    result.put("declarations", parseJsonArray(declarationsJson));
                }
            }

            // Extract statements array
            var stmtStart = json.indexOf("\"statements\":");
            if (stmtStart >= 0) {
                var bracketStart = json.indexOf('[', stmtStart);
                var bracketEnd = findMatchingBracket(json, bracketStart);
                if (bracketStart >= 0 && bracketEnd > bracketStart) {
                    var statementsJson = json.substring(bracketStart + 1, bracketEnd);
                    result.put("statements", parseJsonArray(statementsJson));
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("JSON parsing failed: " + e.getMessage());
            return null;
        }
    }

    private int findMatchingBracket(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private java.util.List<java.util.Map<String, Object>> parseJsonArray(String arrayJson) {
        var result = new java.util.ArrayList<java.util.Map<String, Object>>();
        var objects = arrayJson.split("\\},\\s*\\{");
        for (var obj : objects) {
            obj = obj.replaceAll("^\\s*\\{", "").replaceAll("\\}\\s*$", "");
            if (!obj.trim().isEmpty()) {
                result.add(parseJsonObject(obj));
            }
        }
        return result;
    }

    private java.util.Map<String, Object> parseJsonObject(String objJson) {
        var result = new java.util.HashMap<String, Object>();
        var pairs = objJson.split(",\\s*");
        for (var pair : pairs) {
            var colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                var key = pair.substring(0, colonIdx).trim().replaceAll("\"", "");
                var value = pair.substring(colonIdx + 1).trim().replaceAll("\"", "");
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Process declaration from parsed map
     */
    private void processDeclarationFromMap(java.util.Map<?,?> declMap, CodeGenVisitor visitor) {
        visitor.visitVariableDeclaration(declMap);
    }

    /**
     * Process statement from parsed map
     */
    private void processStatementFromMap(java.util.Map<?,?> stmtMap, WasmCodeGenerator generator) {
        var type = stmtMap.get("type");
        if ("print".equals(type)) {
            // Handle print statement with expressions
            var expressions = stmtMap.get("expressions");
            if (expressions instanceof java.util.List) {
                for (var expr : (java.util.List<?>) expressions) {
                    if (expr instanceof java.util.Map) {
                        processExpressionFromMap((java.util.Map<?,?>) expr, generator);
                        // Assume integer for now
                        generator.emit("  call $print_int");
                    }
                }
            }
        } else if ("assignment".equals(type)) {
            // Handle assignment: target = value
            var target = stmtMap.get("target");
            var value = stmtMap.get("value");

            // Evaluate value first
            if (value instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) value, generator);
            }

            // Store to target
            if (target instanceof java.util.Map) {
                var targetMap = (java.util.Map<?,?>) target;
                var targetType = targetMap.get("type");
                if ("variable_access".equals(targetType)) {
                    var varName = targetMap.get("name");
                    generator.emitStore((String) varName);
                }
            }
        }
    }

    /**
     * Process expression from parsed map
     */
    private void processExpressionFromMap(java.util.Map<?,?> exprMap, WasmCodeGenerator generator) {
        var type = exprMap.get("type");
        if ("integer_literal".equals(type)) {
            var value = exprMap.get("value");
            generator.emitConstant("integer", (String) value);
        } else if ("real_literal".equals(type)) {
            var value = exprMap.get("value");
            generator.emitConstant("real", (String) value);
        } else if ("boolean_literal".equals(type)) {
            var value = exprMap.get("value");
            generator.emitConstant("boolean", "true".equals(value) ? "1" : "0");
        } else if ("variable_access".equals(type)) {
            var name = exprMap.get("name");
            generator.emitLoad((String) name);
        } else if ("binary_op".equals(type)) {
            var left = exprMap.get("left");
            var right = exprMap.get("right");
            var op = exprMap.get("op");
            var operandType = exprMap.get("operandType");

            if (left instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) left, generator);
            }
            if (right instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) right, generator);
            }

            generator.emitBinaryOp((String) op, (String) operandType);
        } else if ("unary_op".equals(type)) {
            var operand = exprMap.get("operand");
            var op = exprMap.get("op");
            var operandType = exprMap.get("operandType");

            if (operand instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) operand, generator);
            }

            generator.emitUnaryOp((String) op, (String) operandType);
        } else if ("array_access".equals(type)) {
            var array = exprMap.get("array");
            var index = exprMap.get("index");

            // Load array base pointer
            if (array instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) array, generator);
            }

            // Calculate address: base + (index-1)*element_size + 4 (skip size field)
            if (index instanceof java.util.Map) {
                processExpressionFromMap((java.util.Map<?,?>) index, generator);
            }
            generator.emit("  i32.const 1");
            generator.emit("  i32.sub"); // 1-based to 0-based indexing
            generator.emit("  i32.const 4"); // element size
            generator.emit("  i32.mul");
            generator.emit("  i32.const 4"); // skip size field
            generator.emit("  i32.add");
            generator.emit("  i32.add");

            // Load value from memory
            generator.emit("  i32.load");
        }
    }

    /**
     * Create a minimal working WASM module for testing
     */
    private String createMinimalWorkingModule() {
        return "(module\n" +
               "  (import \"wasi_snapshot_preview1\" \"proc_exit\"\n" +
               "    (func $proc_exit (param i32)))\n" +
               "  (func $_start\n" +
               "    i32.const 0\n" +
               "    call $proc_exit\n" +
               "  )\n" +
               "  (export \"_start\" (func $_start))\n" +
               ")\n";
    }

    /**
     * Create a minimal WASM module for testing
     */
    private String createMinimalModule() {
        return "(module\n" +
               "  (import \"wasi_snapshot_preview1\" \"fd_write\"\n" +
               "    (func $fd_write (param i32 i32 i32 i32) (result i32)))\n" +
               "  (import \"wasi_snapshot_preview1\" \"proc_exit\"\n" +
               "    (func $proc_exit (param i32)))\n" +
               "  (memory 1)\n" +
               "  (export \"memory\" (memory 0))\n" +
               "  (global $heap_ptr (mut i32) (i32.const 0x1000))\n" +
               "  (func $_start\n" +
               "    call $proc_exit\n" +
               "  )\n" +
               "  (export \"_start\" (func $_start))\n" +
               ")\n";
    }

    /**
     * Get debug information
     */
    public String getDebugInfo() {
        return getASTAsJson(astPointer);
    }
}

