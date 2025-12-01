package com.languagei.compiler.codegen;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.semantic.*;
import java.io.*;
import java.util.*;

/**
 * Generates WebAssembly code from AST
 */
public class CodeGenerator implements ASTVisitor {
    private final WATWriter writer;
    private final VariableScopeManager scopeManager;
    private final TypeResolver typeResolver;
    private final TypeEnvironment typeEnvironment;
    private final FunctionEnvironment functionEnvironment;
    private final Map<String, RecordTypeNode> recordVarTypes;
    private final Map<String, ASTNode> variableTypeAsts;
    private String currentFunction;
    private final List<String> functions;
    private final StringBuilder functionDefs;
    private String lastVariable;
    private Type currentExpressionType;

    public CodeGenerator(Writer output) throws IOException {
        this.writer = new WATWriter(output);
        this.scopeManager = new VariableScopeManager();
        this.typeResolver = new TypeResolver(this.scopeManager);
        this.typeEnvironment = new TypeEnvironment();
        this.functionEnvironment = new FunctionEnvironment();
        this.functions = new ArrayList<>();
        this.functionDefs = new StringBuilder();
        this.recordVarTypes = new HashMap<>();
        this.variableTypeAsts = new HashMap<>();
    }

    public void generate(ProgramNode program) throws IOException {
        // Start module
        writer.writeLine("(module");
        writer.indent();

        // Runtime functions with imports (MUST be first)
        writeRuntimeFunctions();

        // Memory section (exported as "memory" for WASI)
        writer.writeLine("(memory (export \"memory\") 1)");

        // Data section for string output buffer
        writer.writeLine("(data (i32.const 1024) \"\\00\\00\\00\\00\\00\\00\\00\\00\")"); // Space for output buffer
        writer.writeLine("(data (i32.const 2048) \"\\00\\00\\00\\00\\00\\00\\00\\00\")"); // Space for iovs

        // Globals
        writer.writeLine("(global $heap_ptr (mut i32) (i32.const 0))");

        // Generate the program (includes user-defined functions and main)
        program.accept(this);

        // Export the WASI entry point so that the runtime invokes our main wrapper
        writer.writeLine("(export \"_start\" (func $_start))");

        // Add remaining runtime functions after user code
        writeRemainingRuntimeFunctions();

        writer.dedent();
        writer.writeLine(")");
        writer.flush();
    }

    private void writeRuntimeFunctions() throws IOException {
        // Import WASI functions (must be first in module)
        writer.writeLine("(import \"wasi_snapshot_preview1\" \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))");
        writer.writeLine("(import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))");

        // All other functions will be added later
    }

    private void writeRemainingRuntimeFunctions() throws IOException {
        // print_int function - convert integer to string and write it using WASI
        writer.writeLine("(func $print_int (param $val i32)");
        writer.indent();
        writer.writeLine("(call $int_to_string (local.get $val))");
        writer.writeLine("(call $write_string)");
        writer.dedent();
        writer.writeLine(")");

        // print_real function (simplified - just print as int for now)
        writer.writeLine("(func $print_real (param $val f64)");
        writer.indent();
        writer.writeLine("(i32.trunc_f64_s (local.get $val))");
        writer.writeLine("(call $print_int)");
        writer.dedent();
        writer.writeLine(")");

        // print_bool function
        writer.writeLine("(func $print_bool (param $val i32)");
        writer.indent();
        writer.writeLine("(if (local.get $val) (then (call $print_int (i32.const 1))) (else (call $print_int (i32.const 0))))");
        writer.dedent();
        writer.writeLine(")");

        // int_to_string function - convert int to string in memory
        writer.writeLine("(func $int_to_string (param $num i32)");
        writer.indent();
        writer.writeLine("(local $ptr i32)");
        writer.writeLine("(local $temp i32)");
        writer.writeLine("(local $digits i32)");
        writer.writeLine("(local.set $ptr (i32.const 1024))");
        writer.writeLine("(local.set $digits (i32.const 0))");
        writer.writeLine("(if (i32.eqz (local.get $num)) (then");
        writer.indent();
        writer.writeLine("(i32.store8 (local.get $ptr) (i32.const 48))"); // '0'
        writer.writeLine("(local.set $ptr (i32.add (local.get $ptr) (i32.const 1)))");
        writer.writeLine("(local.set $digits (i32.const 1))");
        writer.dedent();
        writer.writeLine("))");
        writer.writeLine("(block $break");
        writer.indent();
        writer.writeLine("(loop $loop");
        writer.indent();
        writer.writeLine("(br_if $break (i32.eqz (local.get $num)))");
        writer.writeLine("(local.set $temp (i32.rem_s (local.get $num) (i32.const 10)))");
        writer.writeLine("(local.set $temp (i32.add (local.get $temp) (i32.const 48)))");
        writer.writeLine("(i32.store8 (local.get $ptr) (local.get $temp))");
        writer.writeLine("(local.set $ptr (i32.add (local.get $ptr) (i32.const 1)))");
        writer.writeLine("(local.set $digits (i32.add (local.get $digits) (i32.const 1)))");
        writer.writeLine("(local.set $num (i32.div_s (local.get $num) (i32.const 10)))");
        writer.writeLine("(br $loop)");
        writer.dedent();
        writer.writeLine(")");
        writer.dedent();
        writer.writeLine(")");
        writer.writeLine("(call $reverse_string (i32.const 1024) (local.get $digits))");
        // Null-terminate the string so that string_length stops after the current number
        writer.writeLine("(i32.store8 (i32.add (i32.const 1024) (local.get $digits)) (i32.const 0))");
        writer.dedent();
        writer.writeLine(")");

        // reverse_string function
        writer.writeLine("(func $reverse_string (param $ptr i32) (param $len i32)");
        writer.indent();
        writer.writeLine("(local $i i32)");
        writer.writeLine("(local $j i32)");
        writer.writeLine("(local $temp i32)");
        writer.writeLine("(local.set $i (i32.const 0))");
        writer.writeLine("(local.set $j (i32.sub (local.get $len) (i32.const 1)))");
        writer.writeLine("(block $break");
        writer.indent();
        writer.writeLine("(loop $loop");
        writer.indent();
        writer.writeLine("(br_if $break (i32.ge_s (local.get $i) (local.get $j)))");
        writer.writeLine("(local.set $temp (i32.load8_u (i32.add (local.get $ptr) (local.get $i))))");
        writer.writeLine("(i32.store8 (i32.add (local.get $ptr) (local.get $i)) (i32.load8_u (i32.add (local.get $ptr) (local.get $j))))");
        writer.writeLine("(i32.store8 (i32.add (local.get $ptr) (local.get $j)) (local.get $temp))");
        writer.writeLine("(local.set $i (i32.add (local.get $i) (i32.const 1)))");
        writer.writeLine("(local.set $j (i32.sub (local.get $j) (i32.const 1)))");
        writer.writeLine("(br $loop)");
        writer.dedent();
        writer.writeLine(")");
        writer.dedent();
        writer.writeLine(")");
        writer.dedent();
        writer.writeLine(")");

        // write_string function
        writer.writeLine("(func $write_string");
        writer.indent();
        writer.writeLine("(local $iovs_ptr i32)");
        writer.writeLine("(local $str_len i32)");
        writer.writeLine("(local.set $iovs_ptr (i32.const 2048))"); // iovs array
        writer.writeLine("(local.set $str_len (call $string_length (i32.const 1024)))");
        writer.writeLine("(i32.store (local.get $iovs_ptr) (i32.const 1024))"); // ptr to string
        writer.writeLine("(i32.store (i32.add (local.get $iovs_ptr) (i32.const 4)) (local.get $str_len))"); // length
        writer.writeLine("(call $fd_write");
        writer.indent();
        writer.writeLine("(i32.const 1)"); // stdout
        writer.writeLine("(local.get $iovs_ptr)");
        writer.writeLine("(i32.const 1)"); // number of iovs
        writer.writeLine("(i32.const 0)"); // nwritten ptr
        writer.dedent();
        writer.writeLine(")");
        writer.writeLine("(drop)"); // ignore result
        writer.dedent();
        writer.writeLine(")");

        // print_char function - print a single ASCII character
        writer.writeLine("(func $print_char (param $ch i32)");
        writer.indent();
        // Store character and null-terminate buffer at 1024, then reuse write_string
        writer.writeLine("(i32.store8 (i32.const 1024) (local.get $ch))");
        writer.writeLine("(i32.store8 (i32.add (i32.const 1024) (i32.const 1)) (i32.const 0))");
        writer.writeLine("(call $write_string)");
        writer.dedent();
        writer.writeLine(")");

        // string_length function
        writer.writeLine("(func $string_length (param $ptr i32) (result i32)");
        writer.indent();
        writer.writeLine("(local $len i32)");
        writer.writeLine("(local.set $len (i32.const 0))");
        writer.writeLine("(block $break");
        writer.indent();
        writer.writeLine("(loop $loop");
        writer.indent();
        writer.writeLine("(br_if $break (i32.eqz (i32.load8_u (i32.add (local.get $ptr) (local.get $len)))))");
        writer.writeLine("(local.set $len (i32.add (local.get $len) (i32.const 1)))");
        writer.writeLine("(br $loop)");
        writer.dedent();
        writer.writeLine(")");
        writer.dedent();
        writer.writeLine(")");
        writer.writeLine("(local.get $len)");
        writer.dedent();
        writer.writeLine(")");

        // allocate_array function - allocate memory for array
        // param $size: number of elements
        // param $element_size: size of each element in bytes
        // result: pointer to allocated array
        writer.writeLine("(func $allocate_array (param $size i32) (param $element_size i32) (result i32)");
        writer.indent();
        writer.writeLine("(local $total_bytes i32)");
        writer.writeLine("(local $array_ptr i32)");

        // Calculate total bytes needed: size * element_size
        writer.writeLine("(local.set $total_bytes (i32.mul (local.get $size) (local.get $element_size)))");

        // Get current heap pointer
        writer.writeLine("(local.set $array_ptr (global.get $heap_ptr))");

        // Update heap pointer
        writer.writeLine("(global.set $heap_ptr (i32.add (global.get $heap_ptr) (local.get $total_bytes)))");

        // Return array pointer
        writer.writeLine("(local.get $array_ptr)");
        writer.dedent();
        writer.writeLine(")");

        // allocate_record function - allocate memory for record
        // param $size: size of record in bytes
        // result: pointer to allocated record
        writer.writeLine("(func $allocate_record (param $size i32) (result i32)");
        writer.indent();
        writer.writeLine("(local $record_ptr i32)");

        // Get current heap pointer
        writer.writeLine("(local.set $record_ptr (global.get $heap_ptr))");

        // Update heap pointer
        writer.writeLine("(global.set $heap_ptr (i32.add (global.get $heap_ptr) (local.get $size)))");

        // Return record pointer
        writer.writeLine("(local.get $record_ptr)");
        writer.dedent();
        writer.writeLine(")");
    }

    @Override
    public void visit(ProgramNode node) {
        // Process type declarations first
        for (ASTNode decl : node.getDeclarations()) {
            if (decl instanceof TypeDeclarationNode) {
                decl.accept(this);
            }
        }

        // Collect all function declarations (forward and full)
        for (ASTNode decl : node.getDeclarations()) {
            if (decl instanceof RoutineDeclarationNode) {
                RoutineDeclarationNode routine = (RoutineDeclarationNode) decl;
                if (routine.getBody() == null) {
                    // Forward declaration - just register
                    functionEnvironment.addForwardDeclaration(routine);
                } else {
                    // Full definition - register and generate
                    functionEnvironment.addFunctionDefinition(routine);
                    visit(routine);
                }
            }
        }

        // Then generate main function with variable declarations and statements
        try {
            currentFunction = "main";
            scopeManager.resetForNewFunction();
            recordVarTypes.clear();
            variableTypeAsts.clear();

            // Collect all local variables from declarations (variables only, functions handled separately) and statements
            for (ASTNode decl : node.getDeclarations()) {
                if (decl instanceof VariableDeclarationNode) {
                    collectLocalVariables(decl);
                }
                // RoutineDeclarationNode handled separately above
            }
            for (ASTNode stmt : node.getStatements()) {
                collectLocalVariables(stmt);
            }

            writer.writeLine(";; Main entry point");
            writer.writeOpenParen("func $_start");

            // Generate local variable declarations (deduplicated by name)
            if (!scopeManager.getFunctionLocals().isEmpty()) {
                writer.writeLine(";; Local variables");
                java.util.Set<String> emitted = new java.util.HashSet<>();
                for (VariableScopeManager.VariableInfo local : scopeManager.getFunctionLocals()) {
                    if (emitted.add(local.name)) {
                        writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
                    }
                }
            }

            // Check if there's a main routine
            boolean hasMain = false;
            for (ASTNode decl : node.getDeclarations()) {
                if (decl instanceof RoutineDeclarationNode) {
                    RoutineDeclarationNode routine = (RoutineDeclarationNode) decl;
                    if ("main".equals(routine.getName())) {
                        hasMain = true;
                        break;
                    }
                }
            }

            if (hasMain) {
                // For programs with an explicit main, preserve existing behaviour:
                // initialize all global variables, then call main.
                for (ASTNode decl : node.getDeclarations()) {
                    if (decl instanceof VariableDeclarationNode) {
                        decl.accept(this);
                    }
                }
                writer.writeLine("(call $main)");
            } else {
                // Programs without main: execute top-level declarations (with initializers)
                // and statements strictly in source order. This is important for tests like
                // array_stats.i, where array element assignments must happen before calls to
                // sum_array/min_array/max_array.

                java.util.List<ASTNode> topLevel = new java.util.ArrayList<>();

                // Include only variable declarations; type and routine declarations are
                // handled separately above and do not produce runtime code.
                for (ASTNode decl : node.getDeclarations()) {
                    if (decl instanceof VariableDeclarationNode) {
                        topLevel.add(decl);
                    }
                }

                // Include all top-level statements (assignments, prints, etc.)
                topLevel.addAll(node.getStatements());

                // Sort by source position to reconstruct original order
                topLevel.sort((a, b) -> {
                    com.languagei.compiler.lexer.Position pa = a.getPosition();
                    com.languagei.compiler.lexer.Position pb = b.getPosition();
                    int lineCmp = Integer.compare(pa.getLine(), pb.getLine());
                    if (lineCmp != 0) return lineCmp;
                    int colCmp = Integer.compare(pa.getColumn(), pb.getColumn());
                    if (colCmp != 0) return colCmp;
                    return Integer.compare(pa.getOffset(), pb.getOffset());
                });

                for (ASTNode n : topLevel) {
                    n.accept(this);
                }
            }

            // Always terminate the WASI process with exit code 0
            writer.writeLine("(i32.const 0)");
            writer.writeLine("(call $proc_exit)");
            writer.writeCloseParen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(VariableDeclarationNode node) {
        // Variable declarations are handled during collection phase
        // Only generate initialization code if there's an initializer
        try {
            // Resolve type aliases so we can distinguish arrays and records behind TypeReferenceNode
            ASTNode declaredType = node.getType();
            ASTNode resolvedTypeAst = declaredType;
            if (declaredType instanceof TypeReferenceNode) {
                TypeReferenceNode ref = (TypeReferenceNode) declaredType;
                ASTNode aliased = typeEnvironment.resolveType(ref.getName());
                if (aliased != null) {
                    resolvedTypeAst = aliased;
                }
            }

            if (resolvedTypeAst instanceof ArrayTypeNode) {
                // Array variable (including aliases to arrays) - allocate memory
                ArrayTypeNode arrayType = (ArrayTypeNode) resolvedTypeAst;

                if (arrayType.getSizeExpression() != null) {
                    // Fixed-size array - allocate memory: size * element_size
                    arrayType.getSizeExpression().accept(this); // Size expression

                    // Compute element size based on the array's element type
                    int elementSize = getArrayElementSize(arrayType);
                    writer.writeLine("(i32.const " + elementSize + ")");
                    writer.writeLine("(call $allocate_array)");
                    writer.writeLine("(local.set $" + node.getName() + ")");
                }
            } else if (resolvedTypeAst instanceof RecordTypeNode) {
                // Record variable (including aliases to records) - allocate memory
                RecordTypeNode recordType = (RecordTypeNode) resolvedTypeAst;

                // Calculate record size (sum of field sizes)
                int recordSize = calculateRecordSize(recordType);
                writer.writeLine("(i32.const " + recordSize + ")"); // Record size
                writer.writeLine("(call $allocate_record)");
                writer.writeLine("(local.set $" + node.getName() + ")");

                // Remember record type for this variable (for field access offsets)
                recordVarTypes.put(node.getName(), recordType);
            } else if (node.getInitializer() != null) {
                // Regular variable with initializer
                // Determine target type (if explicitly specified as primitive)
                Type targetType = null;
                if (node.getType() instanceof PrimitiveTypeNode) {
                    targetType = typeFromNode(node.getType());
                }

                // Determine source expression type using the type resolver
                Type sourceType = typeResolver.resolveType(node.getInitializer());

                // Generate initializer expression
                node.getInitializer().accept(this);

                // If target and source primitive types differ, insert a conversion
                if (targetType != null && sourceType != null && targetType != sourceType) {
                    generateTypeConversion(sourceType, targetType);
                }

                writer.writeLine("(local.set $" + node.getName() + ")");
            }

            // Track the last global variable for return value
            if (scopeManager.isInGlobalScope()) {
                lastVariable = node.getName();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectLocalVariables(ASTNode node) {
        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varDecl = (VariableDeclarationNode) node;

            String wasmType;
            ASTNode resolvedType = varDecl.getType();

            // Resolve type aliases
            if (resolvedType instanceof TypeReferenceNode) {
                TypeReferenceNode typeRef = (TypeReferenceNode) resolvedType;
                ASTNode aliasedType = typeEnvironment.resolveType(typeRef.getName());
                if (aliasedType != null) {
                    resolvedType = aliasedType;
                }
            }

            if (resolvedType instanceof ArrayTypeNode) {
                // Array type - store as pointer (i32)
                wasmType = "i32";
            } else if (resolvedType instanceof RecordTypeNode) {
                // Record type - store as pointer (i32)
                wasmType = "i32";
            } else if (resolvedType != null) {
                // Explicit non-array type
                Type explicitType = typeFromNode(resolvedType);
                wasmType = typeToWasm(explicitType);
            } else if (varDecl.getInitializer() != null) {
                // Type inference from initializer
                Type inferredType = typeResolver.resolveType(varDecl.getInitializer());
                wasmType = typeToWasm(inferredType);
            } else {
                // No type information - default to integer
                wasmType = "i32";
            }

            // Remember the (possibly alias-resolved) type AST for this variable
            if (resolvedType != null) {
                variableTypeAsts.put(varDecl.getName(), resolvedType);
            }

            scopeManager.declareVariable(varDecl.getName(), wasmType);
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            scopeManager.enterScope();
            for (ASTNode stmt : block.getStatements()) {
                collectLocalVariables(stmt);
            }
            scopeManager.exitScope();
        } else if (node instanceof IfStatementNode) {
            IfStatementNode ifStmt = (IfStatementNode) node;
            scopeManager.enterScope();
            collectLocalVariables(ifStmt.getThenBlock());
            scopeManager.exitScope();
            if (ifStmt.getElseBlock() != null) {
                scopeManager.enterScope();
                collectLocalVariables(ifStmt.getElseBlock());
                scopeManager.exitScope();
            }
        } else if (node instanceof WhileLoopNode) {
            WhileLoopNode whileLoop = (WhileLoopNode) node;
            scopeManager.enterScope();
            collectLocalVariables(whileLoop.getBody());
            scopeManager.exitScope();
        } else if (node instanceof ForLoopNode) {
            ForLoopNode forLoop = (ForLoopNode) node;
            // Loop variable lives in the surrounding scope of the loop.
            // If a variable with the same name already exists, reuse it instead of redeclaring.
            if (scopeManager.lookupVariable(forLoop.getVariable()) == null) {
                scopeManager.declareVariable(forLoop.getVariable(), "i32");
            }
            if (forLoop.getArrayExpr() != null) {
                String indexName = forLoop.getVariable() + "_index";
                if (scopeManager.lookupVariable(indexName) == null) {
                    scopeManager.declareVariable(indexName, "i32");
                }
            }
            // Recurse into loop body; BlockNode handling will manage inner scopes as needed.
            collectLocalVariables(forLoop.getBody());
        }
    }

    @Override
    public void visit(TypeDeclarationNode node) {
        // Register type alias
        typeEnvironment.addTypeAlias(node.getName(), node.getType());
        // Type declarations don't generate runtime code
    }

    @Override
    public void visit(RoutineDeclarationNode node) {
        // Skip forward declarations - they don't generate code
        if (node.getBody() == null) {
            return;
        }

        try {
            currentFunction = node.getName();
            scopeManager.resetForNewFunction();
            recordVarTypes.clear();
            variableTypeAsts.clear();

            writer.writeLine(";; Function " + node.getName());
            writer.writeOpenParen("func $" + node.getName());

            // Parameters
            for (ParameterNode param : node.getParameters()) {
                String wasmType = typeToWasm(typeFromParamNode(param));
                writer.writeLine(String.format("(param $%s %s)", param.getName(), wasmType));
                scopeManager.declareVariable(param.getName(), wasmType);

                // Track parameter type AST (with aliases resolved) for field offset and array element size calculations
                ASTNode paramTypeAst = param.getType();
                if (paramTypeAst instanceof TypeReferenceNode) {
                    TypeReferenceNode ref = (TypeReferenceNode) paramTypeAst;
                    ASTNode aliased = typeEnvironment.resolveType(ref.getName());
                    if (aliased != null) {
                        paramTypeAst = aliased;
                    }
                }
                variableTypeAsts.put(param.getName(), paramTypeAst);

                // Track record-typed parameters so that record field access can compute offsets
                if (paramTypeAst instanceof RecordTypeNode) {
                    recordVarTypes.put(param.getName(), (RecordTypeNode) paramTypeAst);
                }
            }

            // Return type
            if (node.getReturnType() != null) {
                String wasmType = typeToWasm(typeFromNode(node.getReturnType()));
                writer.writeLine("(result " + wasmType + ")");
            }

            // Body - process statements
            if (node.getBody() != null) {
                // Collect all local variables from variable declarations
                collectLocalVariables(node.getBody());
                // Generate local variable declarations (skip parameters) and deduplicate by name
                java.util.Set<String> emitted = new java.util.HashSet<>();
                for (VariableScopeManager.VariableInfo local : scopeManager.getFunctionLocals()) {
                    if (local.localIndex >= node.getParameters().size() && emitted.add(local.name)) { // Skip parameters
                        writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
                    }
                }
                // Generate function body
                node.getBody().accept(this);

                // Ensure there is always a return for functions with a result type,
                // even if control reaches the end without executing an explicit return.
                if (node.getReturnType() != null) {
                    Type retType = typeFromNode(node.getReturnType());
                    if (retType == Type.REAL) {
                        writer.writeLine("(f64.const 0.0)");
                    } else {
                        // INTEGER, BOOLEAN and default fallback use i32
                        writer.writeLine("(i32.const 0)");
                    }
                    writer.writeLine("(return)");
                }
            } else {
                // Forward declaration - no body
                // Just close the function declaration
            }

            writer.writeCloseParen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(PrimitiveTypeNode node) {
        // Type nodes don't generate code
    }

    @Override
    public void visit(ArrayTypeNode node) {
        // Type nodes don't generate code
    }

    @Override
    public void visit(RecordTypeNode node) {
        // Type nodes don't generate code
    }

    @Override
    public void visit(TypeReferenceNode node) {
        // Type references don't generate code
    }

    @Override
    public void visit(BinaryExpressionNode node) {
        try {
            // Determine the type of this expression for instruction selection
            Type exprType = typeResolver.resolveType(node);
            currentExpressionType = exprType;

            // Generate left operand
            node.getLeft().accept(this);
            // Generate right operand
            node.getRight().accept(this);

            String instruction = getBinaryInstruction(node.getOperator(), exprType);
            writer.writeLine("(" + instruction + ")");

            // Reset expression type
            currentExpressionType = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getBinaryInstruction(BinaryExpressionNode.Operator operator, Type exprType) {
        boolean isReal = (exprType == Type.REAL);

        return switch(operator) {
            case PLUS -> isReal ? "f64.add" : "i32.add";
            case MINUS -> isReal ? "f64.sub" : "i32.sub";
            case MULTIPLY -> isReal ? "f64.mul" : "i32.mul";
            case DIVIDE -> isReal ? "f64.div" : "i32.div_s";
            case MODULO -> isReal ? "f64.div" : "i32.rem_s"; // No f64 modulo, use div
            case AND -> "i32.and";
            case OR -> "i32.or";
            case XOR -> "i32.xor";
            case LT -> isReal ? "f64.lt" : "i32.lt_s";
            case LE -> isReal ? "f64.le" : "i32.le_s";
            case GT -> isReal ? "f64.gt" : "i32.gt_s";
            case GE -> isReal ? "f64.ge" : "i32.ge_s";
            case EQ -> isReal ? "f64.eq" : "i32.eq";
            case NE -> isReal ? "f64.ne" : "i32.ne";
        };
    }

    @Override
    public void visit(UnaryExpressionNode node) {
        try {
            if (node.getOperator() == UnaryExpressionNode.Operator.MINUS) {
                // Unary minus: handle integer vs real separately
                Type operandType = typeResolver.resolveType(node.getOperand());
                if (operandType == Type.REAL) {
                    // f64.neg is a native WebAssembly instruction
                    node.getOperand().accept(this);
                    writer.writeLine("(f64.neg)");
                } else {
                    // No i32.neg in WebAssembly; implement as (0 - x)
                    writer.writeLine("(i32.const 0)");
                    node.getOperand().accept(this);
                    writer.writeLine("(i32.sub)");
                }
            } else if (node.getOperator() == UnaryExpressionNode.Operator.NOT) {
                node.getOperand().accept(this);
                writer.writeLine("(i32.eqz)");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(LiteralNode node) {
        try {
            Object value = node.getValue();
            if (value instanceof Integer || value instanceof Long) {
                writer.writeLine("(i32.const " + value + ")");
                currentExpressionType = Type.INTEGER;
            } else if (value instanceof Double || value instanceof Float) {
                writer.writeLine("(f64.const " + value + ")");
                currentExpressionType = Type.REAL;
            } else if (value instanceof Boolean) {
                boolean b = (Boolean) value;
                writer.writeLine("(i32.const " + (b ? 1 : 0) + ")");
                currentExpressionType = Type.BOOLEAN;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(IdentifierNode node) {
        try {
            // Always treat identifiers as locals; the set of locals is determined
            // by collectLocalVariables, and we rely on the name here.
            writer.writeLine("(local.get $" + node.getName() + ")");

            // Determine the expression type using the type resolver
            Type exprType = typeResolver.resolveType(node);
            currentExpressionType = exprType;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(ArrayAccessNode node) {
        try {
            // Calculate address of array element
            visitArrayAccessForStore(node);

            // Load value from calculated address
            writer.writeLine("(i32.load)");

            // Set expression type (assume integer for now)
            currentExpressionType = Type.INTEGER;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(RecordAccessNode node) {
        try {
            // Calculate address of the field within its containing record
            visitRecordAccessForStore(node);

            // Decide whether to treat this access as a pointer (nested record)
            // or as a scalar value that must be loaded.
            ASTNode fieldTypeAst = resolveFieldTypeAst(node);
            if (fieldTypeAst != null && isRecordTypeAst(fieldTypeAst)) {
                // Nested record field: expression value is a pointer to the
                // inlined record; leave address on the stack without loading.
                currentExpressionType = Type.INTEGER; // pointers are i32
            } else {
                // Primitive or non-record field: load the stored value.
                writer.writeLine("(i32.load)");
                currentExpressionType = Type.INTEGER;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void visitRecordAccessForStore(RecordAccessNode node) throws IOException {
        // Get base address of the record
        ASTNode object = node.getObject();
        if (object instanceof ArrayAccessNode) {
            // For arrays of records, generate address of the record element
            visitArrayAccessForStore((ArrayAccessNode) object);
        } else if (object instanceof RecordAccessNode) {
            // Nested record access, e.g. john.home.street or p.job.salary.
            // Recursively compute the base address of the inner record without
            // loading from memory, so that nested records are treated as inlined
            // inside their parent record.
            visitRecordAccessForStore((RecordAccessNode) object);
        } else {
            // For standalone record variables (p.x), the identifier already holds
            // a pointer to the record, so expression evaluation gives us the base
            // address directly.
            object.accept(this);
        }

        // Calculate field offset within the resolved record type
        int fieldOffset = calculateFieldOffset(node);
        writer.writeLine("(i32.const " + fieldOffset + ")");

        // Add base address
        writer.writeLine("(i32.add)");
    }

    private RecordTypeNode resolveRecordTypeFromAst(ASTNode typeNode) {
        // Direct record type
        if (typeNode instanceof RecordTypeNode) {
            return (RecordTypeNode) typeNode;
        }

        // Type alias to a record
        if (typeNode instanceof TypeReferenceNode) {
            TypeReferenceNode ref = (TypeReferenceNode) typeNode;
            ASTNode aliased = typeEnvironment.resolveType(ref.getName());
            if (aliased != null) {
                return resolveRecordTypeFromAst(aliased);
            }
        }

        // Array element type may itself be a record or alias
        if (typeNode instanceof ArrayTypeNode) {
            ArrayTypeNode arr = (ArrayTypeNode) typeNode;
            return resolveRecordTypeFromAst(arr.getElementType());
        }

        return null;
    }

    /** Resolve the declared type AST of the field referenced by a RecordAccessNode. */
    private ASTNode resolveFieldTypeAst(RecordAccessNode node) {
        RecordTypeNode recordType = resolveRecordTypeForObject(node.getObject());
        if (recordType == null || recordType.getFields() == null) {
            return null;
        }

        String fieldName = node.getFieldName();
        for (VariableDeclarationNode field : recordType.getFields()) {
            if (field.getName().equals(fieldName)) {
                return field.getType();
            }
        }
        return null;
    }

    /** Check whether a type AST (possibly a type alias) denotes a record type (but not arrays). */
    private boolean isRecordTypeAst(ASTNode typeAst) {
        if (typeAst instanceof RecordTypeNode) {
            return true;
        }
        if (typeAst instanceof TypeReferenceNode) {
            TypeReferenceNode ref = (TypeReferenceNode) typeAst;
            ASTNode aliased = typeEnvironment.resolveType(ref.getName());
            if (aliased != null) {
                return isRecordTypeAst(aliased);
            }
        }
        return false;
    }

    private int calculateFieldOffset(RecordAccessNode node) {
        // Determine the record type that this field access is applied to
        RecordTypeNode recordType = resolveRecordTypeForObject(node.getObject());
        if (recordType == null) {
            return 0;
        }

        String fieldName = node.getFieldName();
        int offset = 0;

        if (recordType.getFields() != null) {
            for (VariableDeclarationNode field : recordType.getFields()) {
                if (field.getName().equals(fieldName)) {
                    return offset;
                }
                // Accumulate size of preceding fields
                offset += sizeOfTypeAst(field.getType());
            }
        }

        return offset;
    }

    /**
     * Resolve the concrete RecordTypeNode for the given object expression used
     * as the base of a RecordAccessNode. Handles simple identifiers, array
     * elements of record type, and nested record accesses.
     */
    private RecordTypeNode resolveRecordTypeForObject(ASTNode object) {
        if (object instanceof IdentifierNode) {
            String name = ((IdentifierNode) object).getName();
            ASTNode typeAst = variableTypeAsts.get(name);
            if (typeAst != null) {
                return resolveRecordTypeFromAst(typeAst);
            }
        } else if (object instanceof ArrayAccessNode) {
            ArrayAccessNode arrayAccess = (ArrayAccessNode) object;
            ASTNode arrayExpr = arrayAccess.getArray();
            if (arrayExpr instanceof IdentifierNode) {
                String arrayName = ((IdentifierNode) arrayExpr).getName();
                ASTNode arrayTypeAst = variableTypeAsts.get(arrayName);
                if (arrayTypeAst instanceof ArrayTypeNode) {
                    ArrayTypeNode arrayType = (ArrayTypeNode) arrayTypeAst;
                    return resolveRecordTypeFromAst(arrayType.getElementType());
                }
            }
        } else if (object instanceof RecordAccessNode) {
            // Nested record access: first resolve the outer record type, then
            // look up the field type and resolve that to a record type.
            RecordAccessNode recObj = (RecordAccessNode) object;
            RecordTypeNode outerRecord = resolveRecordTypeForObject(recObj.getObject());
            if (outerRecord != null && outerRecord.getFields() != null) {
                String outerFieldName = recObj.getFieldName();
                for (VariableDeclarationNode field : outerRecord.getFields()) {
                    if (field.getName().equals(outerFieldName)) {
                        return resolveRecordTypeFromAst(field.getType());
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void visit(RoutineCallNode node) {
        try {
            for (ASTNode arg : node.getArguments()) {
                arg.accept(this);
            }
            writer.writeLine("(call $" + node.getName() + ")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(AssignmentNode node) {
        try {
            // Determine target and source types for conversion
            Type targetType = getExpressionType(node.getTarget());
            Type sourceType = getExpressionType(node.getValue());

            if (node.getTarget() instanceof IdentifierNode) {
                // Simple variable assignment: evaluate value, convert if needed, then set local
                node.getValue().accept(this);
                if (targetType != sourceType) {
                    generateTypeConversion(sourceType, targetType);
                }
                IdentifierNode id = (IdentifierNode) node.getTarget();
                writer.writeLine("(local.set $" + id.getName() + ")");
            } else if (node.getTarget() instanceof ArrayAccessNode) {
                // Array element assignment: address must be below value on the stack
                ArrayAccessNode arrayAccess = (ArrayAccessNode) node.getTarget();

                // 1) Generate address of the array element (leaves address on stack)
                visitArrayAccessForStore(arrayAccess);

                // 2) Generate value
                node.getValue().accept(this);
                if (targetType != sourceType) {
                    generateTypeConversion(sourceType, targetType);
                }

                // Stack order for i32.store: [..., address, value]
                writer.writeLine("(i32.store)");
            } else if (node.getTarget() instanceof RecordAccessNode) {
                // Record field assignment: address must be below value on the stack
                RecordAccessNode recordAccess = (RecordAccessNode) node.getTarget();

                // 1) Generate address of the record field
                visitRecordAccessForStore(recordAccess);

                // 2) Generate value
                node.getValue().accept(this);
                if (targetType != sourceType) {
                    generateTypeConversion(sourceType, targetType);
                }

                // Store value to the calculated address
                writer.writeLine("(i32.store)");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Type getExpressionType(ASTNode node) {
        // Simple type inference for expressions
        if (node instanceof LiteralNode) {
            LiteralNode literal = (LiteralNode) node;
            Object value = literal.getValue();
            if (value instanceof Integer || value instanceof Long) {
                return Type.INTEGER;
            } else if (value instanceof Double || value instanceof Float) {
                return Type.REAL;
            } else if (value instanceof Boolean) {
                return Type.BOOLEAN;
            }
        } else if (node instanceof IdentifierNode) {
            VariableScopeManager.VariableInfo var = scopeManager.lookupVariable(((IdentifierNode) node).getName());
            if (var != null) {
                return "f64".equals(var.wasmType) ? Type.REAL : Type.INTEGER;
            }
        }
        return Type.INTEGER; // Default
    }

    private void generateTypeConversion(Type fromType, Type toType) throws IOException {
        if (fromType == Type.INTEGER && toType == Type.REAL) {
            // int to real
            writer.writeLine("(f64.convert_i32_s)");
        } else if (fromType == Type.REAL && toType == Type.INTEGER) {
            // real to int (rounding)
            writer.writeLine("(i32.trunc_f64_s)");
        } else if (fromType == Type.BOOLEAN && toType == Type.INTEGER) {
            // bool to int: true -> 1, false -> 0 (already i32)
        } else if (fromType == Type.INTEGER && toType == Type.BOOLEAN) {
            // int to bool: non-zero -> true, zero -> false
            writer.writeLine("(i32.const 0)");
            writer.writeLine("(i32.ne)");
        } else if (fromType == Type.REAL && toType == Type.BOOLEAN) {
            // real to bool: non-zero -> true, zero -> false (not supported)
            // For now, convert to int first then to bool
            writer.writeLine("(i32.trunc_f64_s)");
            writer.writeLine("(i32.const 0)");
            writer.writeLine("(i32.ne)");
        }
        // Other conversions as per the specification table
    }

    private void visitArrayAccessForStore(ArrayAccessNode node) throws IOException {
        // Handle multi-dimensional arrays (array-of-arrays) via flattened indexing
        // when we have a pattern like A[i][j].
        if (node.getArray() instanceof ArrayAccessNode) {
            if (generateFlattenedTwoDimArrayAddress(node)) {
                return;
            }
            // If we couldn't recognize a 2D array pattern, fall back to 1D behavior.
        }

        // 1D array access: base + index * element_size
        // Get array reference
        node.getArray().accept(this);

        // Get index (convert from 1-based to 0-based)
        node.getIndex().accept(this);
        writer.writeLine("(i32.const 1)");
        writer.writeLine("(i32.sub)"); // Convert to 0-based indexing

        // Element size in bytes (may be a record size, not always 4)
        int elementSize = resolveArrayElementSize(node);
        writer.writeLine("(i32.const " + elementSize + ")");
        writer.writeLine("(i32.mul)"); // index * element_size

        // Add base address
        writer.writeLine("(i32.add)");
    }

    private int calculateRecordSize(RecordTypeNode recordType) {
        int totalSize = 0;
        if (recordType.getFields() != null) {
            for (VariableDeclarationNode field : recordType.getFields()) {
                totalSize += sizeOfTypeAst(field.getType());
            }
        }
        return totalSize;
    }

    /** Return the size in bytes of a type described by its AST node. */
    private int sizeOfTypeAst(ASTNode typeAst) {
        if (typeAst instanceof PrimitiveTypeNode) {
            // All primitive scalar types are stored as i32 / f64, but in records we
            // represent them uniformly as 4-byte slots.
            return 4;
        }
        if (typeAst instanceof TypeReferenceNode) {
            TypeReferenceNode ref = (TypeReferenceNode) typeAst;
            ASTNode aliased = typeEnvironment.resolveType(ref.getName());
            if (aliased != null) {
                return sizeOfTypeAst(aliased);
            }
            return 4;
        }
        if (typeAst instanceof RecordTypeNode) {
            // Inline record size
            return calculateRecordSize((RecordTypeNode) typeAst);
        }
        if (typeAst instanceof ArrayTypeNode) {
            // Arrays are represented as pointers
            return 4;
        }
        // Fallback: treat as pointer-sized
        return 4;
    }

    /** Compute the element size in bytes for a given array type. */
    private int getArrayElementSize(ArrayTypeNode arrayType) {
        ASTNode elementType = arrayType.getElementType();
        if (elementType instanceof ArrayTypeNode) {
            // For array-of-arrays, treat each element as an inlined row of the inner
            // array rather than a pointer, so that 2D arrays are flattened in memory.
            ArrayTypeNode inner = (ArrayTypeNode) elementType;
            int cols = getFixedArrayLength(inner);
            int elemSize = sizeOfTypeAst(inner.getElementType());
            return cols * elemSize;
        }
        return sizeOfTypeAst(elementType);
    }

    /** Resolve the element size for an ArrayAccess node based on the array variable type. */
    private int resolveArrayElementSize(ArrayAccessNode node) {
        ASTNode arrayExpr = node.getArray();
        if (arrayExpr instanceof IdentifierNode) {
            String name = ((IdentifierNode) arrayExpr).getName();
            ASTNode typeAst = variableTypeAsts.get(name);
            if (typeAst instanceof ArrayTypeNode) {
                return getArrayElementSize((ArrayTypeNode) typeAst);
            }
        }
        // Default element size (i32)
        return 4;
    }

    /**
     * Attempt to generate a flattened 2D address for an ArrayAccessNode of the
     * form A[i][j], where A is declared as array [R] array [C] T. The resulting
     * address is: base(A) + ((i-1) * C + (j-1)) * sizeof(T).
     *
     * Returns true if the pattern was recognized and code was emitted, false
     * otherwise (in which case the caller should fall back to 1D logic).
     */
    private boolean generateFlattenedTwoDimArrayAddress(ArrayAccessNode node) throws IOException {
        ArrayAccessNode inner = (ArrayAccessNode) node.getArray();
        ASTNode baseExpr = inner.getArray();
        if (!(baseExpr instanceof IdentifierNode)) {
            return false;
        }

        String baseName = ((IdentifierNode) baseExpr).getName();
        ASTNode typeAst = variableTypeAsts.get(baseName);
        if (!(typeAst instanceof ArrayTypeNode)) {
            return false;
        }

        ArrayTypeNode outerType = (ArrayTypeNode) typeAst;
        if (!(outerType.getElementType() instanceof ArrayTypeNode)) {
            return false;
        }

        ArrayTypeNode innerType = (ArrayTypeNode) outerType.getElementType();
        int cols = getFixedArrayLength(innerType);
        int elemSize = sizeOfTypeAst(innerType.getElementType());

        // Compute ((i-1) * cols + (j-1)) * elemSize
        // i index (from inner access A[i])
        inner.getIndex().accept(this);
        writer.writeLine("(i32.const 1)");
        writer.writeLine("(i32.sub)"); // i - 1
        writer.writeLine("(i32.const " + cols + ")");
        writer.writeLine("(i32.mul)"); // (i-1) * cols

        // j index (from outer access [j])
        node.getIndex().accept(this);
        writer.writeLine("(i32.const 1)");
        writer.writeLine("(i32.sub)"); // j - 1
        writer.writeLine("(i32.add)"); // (i-1)*cols + (j-1)

        writer.writeLine("(i32.const " + elemSize + ")");
        writer.writeLine("(i32.mul)"); // offset in bytes

        // Add base pointer. Order (offset, base) is fine because addition is
        // commutative.
        writer.writeLine("(local.get $" + baseName + ")");
        writer.writeLine("(i32.add)");
        return true;
    }

    /** Extract constant length from a fixed-size ArrayTypeNode; fallback to 1. */
    private int getFixedArrayLength(ArrayTypeNode arrayType) {
        if (arrayType.getSizeExpression() instanceof LiteralNode) {
            LiteralNode lit = (LiteralNode) arrayType.getSizeExpression();
            Object v = lit.getValue();
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
        }
        // Fallback for non-constant or unsupported sizes
        return 1;
    }

    @Override
    public void visit(IfStatementNode node) {
        try {
            node.getCondition().accept(this);
            writer.writeLine(";; If statement");
            // If-statement is a pure statement, not an expression: no result value on stack
            writer.writeLine("(if");
            writer.indent();
            writer.writeLine("(then");
            writer.indent();
            scopeManager.enterScope();
            node.getThenBlock().accept(this);
            scopeManager.exitScope();
            writer.dedent();
            writer.writeLine(")");

            if (node.getElseBlock() != null) {
                writer.writeLine("(else");
                writer.indent();
                scopeManager.enterScope();
                node.getElseBlock().accept(this);
                scopeManager.exitScope();
                writer.dedent();
                writer.writeLine(")");
            }

            writer.dedent();
            writer.writeLine(")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(WhileLoopNode node) {
        try {
            writer.writeLine(";; While loop");
            writer.writeLine("(block $break");
            writer.indent();
            writer.writeLine("(loop $continue");
            writer.indent();
            node.getCondition().accept(this);
            writer.writeLine("(i32.eqz)");
            writer.writeLine("(br_if $break)");
            scopeManager.enterScope();
            node.getBody().accept(this);
            scopeManager.exitScope();
            writer.writeLine("(br $continue)");
            writer.dedent();
            writer.writeLine(")");
            writer.dedent();
            writer.writeLine(")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(ForLoopNode node) {
        try {
            if (node.getArrayExpr() != null) {
                // Iterate over array elements
                generateArrayIteration(node);
            } else if (node.getRangeStart() != null) {
                // Range-based iteration
                generateRangeIteration(node);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateArrayIteration(ForLoopNode node) throws IOException {
        // For array iteration, we need:
        // - A loop index variable
        // - Check if index is within array bounds
        // - Set loop variable to array[index]
        // - Increment index

        String loopVar = node.getVariable();
        boolean reverse = node.isReverse();

        Integer arrayLength = getArrayLengthForArrayExpr(node.getArrayExpr());
        int lengthConst = (arrayLength != null ? arrayLength : 5);

        writer.writeLine(";; For loop over array");
        // Initialize index
        if (reverse) {
            writer.writeLine("(i32.const " + lengthConst + ")");
        } else {
            writer.writeLine("(i32.const 1)");
        }
        writer.writeLine("(local.set $" + loopVar + "_index)");

        writer.writeLine("(block $break");
        writer.indent();
        writer.writeLine("(loop $continue");
        writer.indent();

        // Check bounds
        if (reverse) {
            writer.writeLine("(local.get $" + loopVar + "_index)");
            writer.writeLine("(i32.const 1)");
            writer.writeLine("(i32.lt_s)");
            writer.writeLine("(br_if $break)");
        } else {
            writer.writeLine("(local.get $" + loopVar + "_index)");
            writer.writeLine("(i32.const " + lengthConst + ")");
            writer.writeLine("(i32.gt_s)");
            writer.writeLine("(br_if $break)");
        }

        // Set loop variable to array[index]
        node.getArrayExpr().accept(this); // Array reference
        writer.writeLine("(local.get $" + loopVar + "_index)");
        writer.writeLine("(i32.const 1)");
        writer.writeLine("(i32.sub)"); // Convert to 0-based
        writer.writeLine("(i32.const 4)");
        writer.writeLine("(i32.mul)");
        writer.writeLine("(i32.add)");
        writer.writeLine("(i32.load)"); // Load array[index]
        writer.writeLine("(local.set $" + loopVar + ")");

        scopeManager.enterScope();
        node.getBody().accept(this);
        scopeManager.exitScope();

        // Increment/decrement index
        writer.writeLine("(local.get $" + loopVar + "_index)");
        writer.writeLine("(i32.const 1)");
        if (reverse) {
            writer.writeLine("(i32.sub)");
        } else {
            writer.writeLine("(i32.add)");
        }
        writer.writeLine("(local.set $" + loopVar + "_index)");
        writer.writeLine("(br $continue)");

        writer.dedent();
        writer.writeLine(")");
        writer.dedent();
        writer.writeLine(")");
    }

    private void generateRangeIteration(ForLoopNode node) throws IOException {
        if (node.getRangeStart() != null) {
            boolean reverse = node.isReverse();

            if (reverse) {
                node.getRangeEnd().accept(this);
            } else {
                node.getRangeStart().accept(this);
            }
            writer.writeLine("(local.set $" + node.getVariable() + ")");

            writer.writeLine(";; For loop range");
            writer.writeLine("(block $break");
            writer.indent();
            writer.writeLine("(loop $continue");
            writer.indent();

            writer.writeLine("(local.get $" + node.getVariable() + ")");
            if (reverse) {
                node.getRangeStart().accept(this);
                writer.writeLine("(i32.lt_s)");
            } else {
                node.getRangeEnd().accept(this);
                writer.writeLine("(i32.gt_s)");
            }
            writer.writeLine("(br_if $break)");

            scopeManager.enterScope();
            node.getBody().accept(this);
            scopeManager.exitScope();

            writer.writeLine("(local.get $" + node.getVariable() + ")");
            writer.writeLine("(i32.const 1)");
            if (reverse) {
                writer.writeLine("(i32.sub)");
            } else {
                writer.writeLine("(i32.add)");
            }
            writer.writeLine("(local.set $" + node.getVariable() + ")");
            writer.writeLine("(br $continue)");

            writer.dedent();
            writer.writeLine(")");
            writer.dedent();
            writer.writeLine(")");
        }
    }

    private Integer getArrayLengthForArrayExpr(ASTNode arrayExpr) {
        if (arrayExpr instanceof IdentifierNode) {
            String name = ((IdentifierNode) arrayExpr).getName();
            ASTNode typeAst = variableTypeAsts.get(name);
            if (typeAst instanceof ArrayTypeNode) {
                ArrayTypeNode arrayType = (ArrayTypeNode) typeAst;
                ASTNode sizeExpr = arrayType.getSizeExpression();
                if (sizeExpr instanceof LiteralNode) {
                    Object value = ((LiteralNode) sizeExpr).getValue();
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void visit(ReturnStatementNode node) {
        try {
            if (node.getValue() != null) {
                node.getValue().accept(this);
            }
            // Explicit return ensures correct control flow and stack discipline
            writer.writeLine("(return)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(PrintStatementNode node) {
        try {
            java.util.List<ASTNode> exprs = node.getExpressions();
            int count = exprs.size();

            for (int idx = 0; idx < count; idx++) {
                ASTNode expr = exprs.get(idx);
                expr.accept(this);

                // Determine type and call appropriate print function for this expression
                Type exprType = typeResolver.resolveType(expr);
                if (exprType == Type.REAL) {
                    writer.writeLine("(call $print_real)");
                } else if (exprType == Type.BOOLEAN) {
                    writer.writeLine("(call $print_bool)");
                } else {
                    // Default and integers
                    writer.writeLine("(call $print_int)");
                }

                // Separate multiple arguments in a single print statement with a space
                if (idx < count - 1) {
                    writer.writeLine("(i32.const 32)"); // ' '
                    writer.writeLine("(call $print_char)");
                }
            }

            // Each print statement ends with a newline
            writer.writeLine("(i32.const 10)"); // '\n'
            writer.writeLine("(call $print_char)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(BlockNode node) {
        scopeManager.enterScope();
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
        scopeManager.exitScope();
    }

    private String typeToWasm(Type type) {
        if (type == Type.INTEGER) return "i32";
        if (type == Type.REAL) return "f64";
        if (type == Type.BOOLEAN) return "i32";
        return "i32";
    }

    // Type resolution from AST nodes
    private Type typeFromNode(ASTNode node) {
        if (node instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode prim = (PrimitiveTypeNode) node;
            return switch(prim.getType()) {
                case INTEGER -> Type.INTEGER;
                case REAL -> Type.REAL;
                case BOOLEAN -> Type.BOOLEAN;
            };
        }
        // Default fallback
        return Type.INTEGER;
    }

    private Type typeFromParamNode(ParameterNode param) {
        if (param.getType() instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode prim = (PrimitiveTypeNode) param.getType();
            return switch(prim.getType()) {
                case INTEGER -> Type.INTEGER;
                case REAL -> Type.REAL;
                case BOOLEAN -> Type.BOOLEAN;
            };
        }
        return Type.INTEGER;
    }
}

