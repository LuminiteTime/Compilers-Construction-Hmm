package compiler.codegen;

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
        // Process program-level declarations
        // This would iterate through declarations in the AST
    }

    @Override
    public void visitVariableDeclaration(Object node) {
        // Generate code for variable declaration
        // Extract: name, type, initializer from node
    }

    @Override
    public void visitTypeDeclaration(Object node) {
        // Type declarations don't generate code,
        // but are used for semantic information
    }

    @Override
    public void visitRoutineDeclaration(Object node) {
        // Generate function prologue
        // Extract: header, body from node
    }

    @Override
    public void visitRoutineHeader(Object node) {
        // Extract routine metadata:
        // - name
        // - parameters (types and names)
        // - return type
    }

    @Override
    public void visitRoutineBody(Object node) {
        // Generate function body statements
    }

    @Override
    public void visitAssignment(Object node) {
        // Generate assignment code:
        // 1. Evaluate right-hand side expression
        // 2. Store to left-hand side target
    }

    @Override
    public void visitPrintStatement(Object node) {
        // Generate print code for each expression:
        // 1. Evaluate expression
        // 2. Call appropriate print function based on type
    }

    @Override
    public void visitWhileLoop(Object node) {
        // Generate while loop:
        // (block $break
        //   (loop $continue
        //     ;; condition (negated)
        //     br_if $break
        //     ;; body
        //     br $continue
        //   )
        // )
    }

    @Override
    public void visitForLoop(Object node) {
        // Generate for loop:
        // 1. Initialize loop variable
        // 2. Setup block/loop structure
        // 3. Condition check
        // 4. Body
        // 5. Increment/decrement
    }

    @Override
    public void visitIfStatement(Object node) {
        // Generate if statement:
        // (if (result type?)
        //   ;; condition
        //   ;; then-branch
        //   else
        //   ;; else-branch
        // end)
    }

    @Override
    public void visitReturnStatement(Object node) {
        // Generate return code:
        // 1. Evaluate expression (if any)
        // 2. Return instruction
    }

    @Override
    public void visitExpression(Object node) {
        // Dispatch to appropriate expression type
    }

    @Override
    public void visitBinaryOp(Object node) {
        // Generate binary operation:
        // 1. Generate code for left operand
        // 2. Generate code for right operand
        // 3. Emit operation instruction
    }

    @Override
    public void visitUnaryOp(Object node) {
        // Generate unary operation:
        // 1. Generate code for operand
        // 2. Emit unary operation instruction
    }

    @Override
    public void visitIntegerLiteral(Object node) {
        // Emit: i32.const <value>
    }

    @Override
    public void visitRealLiteral(Object node) {
        // Emit: f64.const <value>
    }

    @Override
    public void visitBooleanLiteral(Object node) {
        // Emit: i32.const 0 (false) or i32.const 1 (true)
    }

    @Override
    public void visitStringLiteral(Object node) {
        // Strings are currently not directly supported
        // Would need special handling
    }

    @Override
    public void visitVariableAccess(Object node) {
        // Emit variable load:
        // local.get $<name> (for locals)
        // global.get $<name> (for globals)
    }

    @Override
    public void visitArrayAccess(Object node) {
        // Generate array element access:
        // 1. Load base pointer
        // 2. Calculate address: base + (index-1)*element_size + 4 (skip size)
        // 3. Load element value
    }

    @Override
    public void visitFieldAccess(Object node) {
        // Generate record field access:
        // 1. Load record base pointer
        // 2. Add field offset (from symbol table)
        // 3. Load field value
    }

    @Override
    public void visitRoutineCall(Object node) {
        // Generate routine call:
        // 1. Evaluate and push each argument
        // 2. Emit call instruction
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

