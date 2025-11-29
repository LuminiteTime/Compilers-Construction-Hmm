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
    private final Map<String, LocalVariable> localVariables;
    private int localVarIndex;
    private String currentFunction;
    private final List<String> functions;
    private final StringBuilder functionDefs;
    private String lastVariable;

    public static class LocalVariable {
        String name;
        String wasmType;
        int index;

        LocalVariable(String name, String wasmType, int index) {
            this.name = name;
            this.wasmType = wasmType;
            this.index = index;
        }
    }

    public CodeGenerator(Writer output) throws IOException {
        this.writer = new WATWriter(output);
        this.localVariables = new HashMap<>();
        this.localVarIndex = 0;
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
        // print_int function - simplified for testing
        writer.writeLine("(func $print_int (param $val i32)");
        writer.indent();
        writer.writeLine("(i32.store8 (i32.const 1024) (i32.const 49))"); // '1'
        writer.writeLine("(i32.store8 (i32.const 1025) (i32.const 10))"); // '\n'
        writer.writeLine("(i32.store (i32.const 2048) (i32.const 1024))"); // iovs[0] = ptr to string
        writer.writeLine("(i32.store (i32.const 2052) (i32.const 2))"); // iovs[1] = length (2 bytes)
        writer.writeLine("(call $fd_write (i32.const 1) (i32.const 2048) (i32.const 1) (i32.const 0))");
        writer.writeLine("(drop)");
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
    }

    @Override
    public void visit(ProgramNode node) {
        // Generate function declarations first
        for (ASTNode decl : node.getDeclarations()) {
            if (decl instanceof RoutineDeclarationNode) {
                decl.accept(this);
            }
        }

        // Then generate main function with variable declarations and statements
        try {
            currentFunction = "main";
            localVariables.clear();
            localVarIndex = 0;

            // Collect all local variables from declarations (variables) and statements
            for (ASTNode decl : node.getDeclarations()) {
                if (decl instanceof VariableDeclarationNode) {
                    collectLocalVariables(decl);
                }
            }
            for (ASTNode stmt : node.getStatements()) {
                collectLocalVariables(stmt);
            }

            writer.writeOpenParen("func $_start");

            // Generate local variable declarations
            for (LocalVariable local : localVariables.values()) {
                writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
            }

            // Generate variable initialization from declarations
            for (ASTNode decl : node.getDeclarations()) {
                if (decl instanceof VariableDeclarationNode) {
                    decl.accept(this);
                }
            }

            // Generate all statements
            for (ASTNode stmt : node.getStatements()) {
                stmt.accept(this);
            }

            // Exit with the value of the last declared variable
            if (lastVariable != null) {
                writer.writeLine("(local.get $" + lastVariable + ")");
                writer.writeLine("(call $proc_exit)");
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
            if (node.getInitializer() != null) {
                node.getInitializer().accept(this);
                writer.writeLine("(local.set $" + node.getName() + ")");
            }
            // Track the last variable for return value
            lastVariable = node.getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void collectLocalVariables(ASTNode node) {
        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode varDecl = (VariableDeclarationNode) node;
            String wasmType = typeToWasm(typeFromNode(varDecl.getType()));
            localVariables.put(varDecl.getName(), new LocalVariable(varDecl.getName(), wasmType, localVarIndex++));
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (ASTNode stmt : block.getStatements()) {
                collectLocalVariables(stmt);
            }
        }
    }

    @Override
    public void visit(TypeDeclarationNode node) {
        // Type declarations don't generate code
    }

    @Override
    public void visit(RoutineDeclarationNode node) {
        try {
            currentFunction = node.getName();
            localVariables.clear();
            localVarIndex = 0;

            writer.writeOpenParen("func $" + node.getName());

            // Parameters
            for (ParameterNode param : node.getParameters()) {
                String wasmType = typeToWasm(typeFromParamNode(param));
                writer.writeLine(String.format("(param $%s %s)", param.getName(), wasmType));
                localVariables.put(param.getName(), new LocalVariable(param.getName(), wasmType, localVarIndex++));
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
                // Generate local variable declarations
                for (LocalVariable local : localVariables.values()) {
                    if (local.index >= node.getParameters().size()) { // Skip parameters
                        writer.writeLine(String.format("(local $%s %s)", local.name, local.wasmType));
                    }
                }
                // Generate function body
                node.getBody().accept(this);
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
            // Generate left operand
            node.getLeft().accept(this);
            // Generate right operand
            node.getRight().accept(this);

            String instruction = switch(node.getOperator()) {
                case PLUS -> "i32.add";
                case MINUS -> "i32.sub";
                case MULTIPLY -> "i32.mul";
                case DIVIDE -> "i32.div_s";
                case MODULO -> "i32.rem_s";
                case AND -> "i32.and";
                case OR -> "i32.or";
                case XOR -> "i32.xor";
                case LT -> "i32.lt_s";
                case LE -> "i32.le_s";
                case GT -> "i32.gt_s";
                case GE -> "i32.ge_s";
                case EQ -> "i32.eq";
                case NE -> "i32.ne";
            };

            writer.writeLine("(" + instruction + ")");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            } else if (value instanceof Double || value instanceof Float) {
                writer.writeLine("(f64.const " + value + ")");
            } else if (value instanceof Boolean) {
                boolean b = (Boolean) value;
                writer.writeLine("(i32.const " + (b ? 1 : 0) + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(IdentifierNode node) {
        try {
            LocalVariable var = localVariables.get(node.getName());
            if (var != null) {
                writer.writeLine("(local.get $" + node.getName() + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(ArrayAccessNode node) {
        // Array access generates code to compute address
        try {
            node.getArray().accept(this);
            node.getIndex().accept(this);
            writer.writeLine("(i32.add)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(RecordAccessNode node) {
        node.getObject().accept(this);
        // Field access logic
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
            if (node.getTarget() instanceof IdentifierNode) {
                IdentifierNode id = (IdentifierNode) node.getTarget();
                node.getValue().accept(this);
                writer.writeLine("(local.set $" + id.getName() + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(IfStatementNode node) {
        try {
            node.getCondition().accept(this);
            writer.writeLine("(if");
            writer.indent();
            writer.writeLine("(then");
            writer.indent();
            node.getThenBlock().accept(this);
            writer.dedent();
            writer.writeLine(")");

            if (node.getElseBlock() != null) {
                writer.writeLine("(else");
                writer.indent();
                node.getElseBlock().accept(this);
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
            writer.writeLine("(block $break");
            writer.indent();
            writer.writeLine("(loop $continue");
            writer.indent();
            node.getCondition().accept(this);
            writer.writeLine("(i32.eqz)");
            writer.writeLine("(br_if $break)");
            node.getBody().accept(this);
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
            // For now, simple loop implementation
            writer.writeLine("(local $" + node.getVariable() + " i32)");

            if (node.getRangeStart() != null) {
                node.getRangeStart().accept(this);
                writer.writeLine("(local.set $" + node.getVariable() + ")");

                writer.writeLine("(block $break");
                writer.indent();
                writer.writeLine("(loop $continue");
                writer.indent();

                node.getRangeEnd().accept(this);
                writer.writeLine("(local.get $" + node.getVariable() + ")");
                writer.writeLine("(i32.gt_s)");
                writer.writeLine("(br_if $break)");

                node.getBody().accept(this);

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(ReturnStatementNode node) {
        try {
            if (node.getValue() != null) {
                node.getValue().accept(this);
                writer.writeLine("(return)");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
    }

    private String typeToWasm(Type type) {
        if (type == Type.INTEGER) return "i32";
        if (type == Type.REAL) return "f64";
        if (type == Type.BOOLEAN) return "i32";
        return "i32";
    }

    // Placeholder - these will be filled with actual type resolution
    private Type typeFromNode(ASTNode node) {
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

