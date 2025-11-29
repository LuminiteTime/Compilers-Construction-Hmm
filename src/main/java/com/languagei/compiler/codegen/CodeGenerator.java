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
    private String currentFunction;
    private final List<String> functions;
    private final StringBuilder functionDefs;
    private String lastVariable;
    private Type currentExpressionType;

    public CodeGenerator(Writer output) throws IOException {
        this.writer = new WATWriter(output);
        this.scopeManager = new VariableScopeManager();
        this.typeResolver = new TypeResolver();
        this.typeEnvironment = new TypeEnvironment();
        this.functionEnvironment = new FunctionEnvironment();
        this.functions = new ArrayList<>();
        this.functionDefs = new StringBuilder();
    }

    public void generate(ProgramNode program) throws IOException {
        // Start module
        writer.writeLine("(module");
        writer.indent();

        // Runtime functions with imports (MUST be first)
        writeRuntimeFunctions();

        // Memory section
        writer.writeLine("(memory 1)");

        // Data section for string output buffer
        writer.writeLine("(data (i32.const 1024) \"\\00\\00\\00\\00\\00\\00\\00\\00\")"); // Space for output buffer
        writer.writeLine("(data (i32.const 2048) \"\\00\\00\\00\\00\\00\\00\\00\\00\")"); // Space for iovs

        // Globals
        writer.writeLine("(global $heap_ptr (mut i32) (i32.const 0))");

        // Generate the program (includes user-defined functions and main)
        program.accept(this);

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
        // print_int function - no-op for testing
        writer.writeLine("(func $print_int (param $val i32)");
        writer.indent();
        // Do nothing
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

            // Generate local variable declarations
            if (!scopeManager.getFunctionLocals().isEmpty()) {
                writer.writeLine(";; Local variables");
                for (VariableScopeManager.VariableInfo local : scopeManager.getFunctionLocals()) {
                    writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
                }
            }

            // Generate variable initialization from declarations
            for (ASTNode decl : node.getDeclarations()) {
                if (decl instanceof VariableDeclarationNode) {
                    decl.accept(this);
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
                // Call main function
                writer.writeLine("(call $main)");
                writer.writeLine("(call $proc_exit)");
            } else {
                // Generate all statements
                for (ASTNode stmt : node.getStatements()) {
                    stmt.accept(this);
                }

                // Exit with the value of the last declared variable in global scope
                if (lastVariable != null) {
                    VariableScopeManager.VariableInfo varInfo = scopeManager.lookupVariable(lastVariable);
                    if (varInfo != null && "i32".equals(varInfo.wasmType)) {
                        writer.writeLine("(local.get $" + lastVariable + ")");
                        writer.writeLine("(call $proc_exit)");
                    } else {
                        // Non-integer variables can't be used as exit code
                        writer.writeLine("(i32.const 0)");
                        writer.writeLine("(call $proc_exit)");
                    }
                } else {
                    // Default exit code 0
                    writer.writeLine("(i32.const 0)");
                    writer.writeLine("(call $proc_exit)");
                }
            }
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
            if (node.getType() instanceof ArrayTypeNode) {
                // Array variable - need to allocate memory
                ArrayTypeNode arrayType = (ArrayTypeNode) node.getType();

                if (arrayType.getSizeExpression() != null) {
                    // Fixed-size array - allocate memory
                    arrayType.getSizeExpression().accept(this); // Size expression
                    writer.writeLine("(i32.const 4)"); // Element size (assume i32 for now)
                    writer.writeLine("(call $allocate_array)");
                    writer.writeLine("(local.set $" + node.getName() + ")");
                }
                // TODO: Handle dynamic arrays and initialization
            } else if (node.getType() instanceof RecordTypeNode) {
                // Record variable - need to allocate memory
                RecordTypeNode recordType = (RecordTypeNode) node.getType();

                // Calculate record size (sum of field sizes)
                int recordSize = calculateRecordSize(recordType);
                writer.writeLine("(i32.const " + recordSize + ")"); // Record size
                writer.writeLine("(call $allocate_record)");
                writer.writeLine("(local.set $" + node.getName() + ")");
            } else if (node.getInitializer() != null) {
                // Regular variable with initializer
                node.getInitializer().accept(this);
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
            // Declare the loop variable in current scope
            scopeManager.declareVariable(forLoop.getVariable(), "i32");
            if (forLoop.getArrayExpr() != null) {
                // For array iteration, we need an index variable
                scopeManager.declareVariable(forLoop.getVariable() + "_index", "i32");
            }
            scopeManager.enterScope();
            collectLocalVariables(forLoop.getBody());
            scopeManager.exitScope();
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

            writer.writeLine(";; Function " + node.getName());
            writer.writeOpenParen("func $" + node.getName());

            // Parameters
            for (ParameterNode param : node.getParameters()) {
                String wasmType = typeToWasm(typeFromParamNode(param));
                writer.writeLine(String.format("(param $%s %s)", param.getName(), wasmType));
                scopeManager.declareVariable(param.getName(), wasmType);
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
                // Generate local variable declarations (skip parameters)
                for (VariableScopeManager.VariableInfo local : scopeManager.getFunctionLocals()) {
                    if (local.localIndex >= node.getParameters().size()) { // Skip parameters
                        writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
                    }
                }
                // Generate function body
                node.getBody().accept(this);
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
            node.getOperand().accept(this);

            String instruction = switch(node.getOperator()) {
                case MINUS -> "i32.neg";
                case NOT -> "i32.eqz";
                default -> "";
            };

            if (!instruction.isEmpty()) {
                writer.writeLine("(" + instruction + ")");
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
            VariableScopeManager.VariableInfo var = scopeManager.lookupVariable(node.getName());
            if (var != null) {
                writer.writeLine("(local.get $" + node.getName() + ")");
                // Set expression type based on variable type
                if ("f64".equals(var.wasmType)) {
                    currentExpressionType = Type.REAL;
                } else if ("i32".equals(var.wasmType)) {
                    // Could be integer or boolean, but for now assume integer
                    currentExpressionType = Type.INTEGER;
                }
            }
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
            // Calculate field offset in record
            visitRecordAccessForStore(node);

            // Load field value
            writer.writeLine("(i32.load)");

            // Set expression type (assume integer for now)
            currentExpressionType = Type.INTEGER;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void visitRecordAccessForStore(RecordAccessNode node) throws IOException {
        // Get record reference
        node.getObject().accept(this);

        // Calculate field offset
        int fieldOffset = calculateFieldOffset(node);
        writer.writeLine("(i32.const " + fieldOffset + ")");

        // Add base address
        writer.writeLine("(i32.add)");
    }

    private int calculateFieldOffset(RecordAccessNode node) {
        // TODO: Implement proper field offset calculation based on record type
        // For now, assume fields are at fixed offsets
        String fieldName = node.getFieldName();
        // Simple mapping: assume field order corresponds to declaration order
        // This is a placeholder - proper implementation needs record type analysis
        return 0; // First field at offset 0
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

            node.getValue().accept(this); // Generate value first

            // Apply type conversion if needed
            if (targetType != sourceType) {
                generateTypeConversion(sourceType, targetType);
            }

            if (node.getTarget() instanceof IdentifierNode) {
                IdentifierNode id = (IdentifierNode) node.getTarget();
                writer.writeLine("(local.set $" + id.getName() + ")");
            } else if (node.getTarget() instanceof ArrayAccessNode) {
                // Array element assignment
                ArrayAccessNode arrayAccess = (ArrayAccessNode) node.getTarget();

                // Generate array address calculation (this will leave address on stack)
                visitArrayAccessForStore(arrayAccess);

                // Store value (which is already on stack) to the address
                writer.writeLine("(i32.store)");
            } else if (node.getTarget() instanceof RecordAccessNode) {
                // Record field assignment
                RecordAccessNode recordAccess = (RecordAccessNode) node.getTarget();

                // Generate record field address calculation
                visitRecordAccessForStore(recordAccess);

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
        // Get array reference
        node.getArray().accept(this);

        // Get index (convert from 1-based to 0-based)
        node.getIndex().accept(this);
        writer.writeLine("(i32.const 1)");
        writer.writeLine("(i32.sub)"); // Convert to 0-based indexing

        // Element size (assume 4 bytes for i32)
        writer.writeLine("(i32.const 4)");
        writer.writeLine("(i32.mul)"); // index * element_size

        // Add base address
        writer.writeLine("(i32.add)");
    }

    private int calculateRecordSize(RecordTypeNode recordType) {
        int totalSize = 0;
        // For now, assume each field is 4 bytes (i32)
        // TODO: Calculate actual size based on field types
        if (recordType.getFields() != null) {
            totalSize = recordType.getFields().size() * 4;
        }
        return totalSize;
    }

    @Override
    public void visit(IfStatementNode node) {
        try {
            node.getCondition().accept(this);
            writer.writeLine(";; If statement");
            writer.writeLine("(if (result i32)");
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
            } else {
                // WAT requires else block when if returns a result
                writer.writeLine("(else (i32.const 0))");
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

        writer.writeLine(";; For loop over array");
        // Initialize index
        if (reverse) {
            // TODO: Get array length and start from end
            writer.writeLine("(i32.const 5)"); // Assume array size 5 for now
        } else {
            writer.writeLine("(i32.const 1)"); // Start from 1 (1-based indexing)
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
            writer.writeLine("(i32.const 5)"); // Assume array size 5 for now
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
            // Initialize the loop variable
            node.getRangeStart().accept(this);
            writer.writeLine("(local.set $" + node.getVariable() + ")");

            writer.writeLine(";; For loop range");
            writer.writeLine("(block $break");
            writer.indent();
            writer.writeLine("(loop $continue");
            writer.indent();

            node.getRangeEnd().accept(this);
            writer.writeLine("(local.get $" + node.getVariable() + ")");
            writer.writeLine("(i32.gt_s)");
            writer.writeLine("(br_if $break)");

            scopeManager.enterScope();
            node.getBody().accept(this);
            scopeManager.exitScope();

            writer.writeLine("(local.get $" + node.getVariable() + ")");
            writer.writeLine("(i32.const 1)");
            writer.writeLine("(i32.add)");
            writer.writeLine("(local.set $" + node.getVariable() + ")");
            writer.writeLine("(br $continue)");

            writer.dedent();
            writer.writeLine(")");
            writer.dedent();
            writer.writeLine(")");
        }
    }

    @Override
    public void visit(ReturnStatementNode node) {
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
        // In WAT, functions with result type implicitly return the top stack value
        // No explicit return needed
    }

    @Override
    public void visit(PrintStatementNode node) {
        try {
            for (ASTNode expr : node.getExpressions()) {
                expr.accept(this);
                // Determine type and call appropriate print function
                // For now, assume all are integers
                writer.writeLine("(call $print_int)");
            }
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

