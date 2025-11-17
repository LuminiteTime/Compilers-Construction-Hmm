package compiler.codegen;

/**
 * Unit tests for code generator components
 * Comprehensive testing of all codegen functionality
 */
public class CodeGenTest {

    // ========== TYPE SYSTEM TESTS ==========

    public static void testWasmType() {
        System.out.println("\n=== Testing WasmType ===");
        
        // Test type mapping
        assert WasmType.I32.getWasmName().equals("i32");
        assert WasmType.F64.getWasmName().equals("f64");
        System.out.println("✓ WASM type names correct");
        
        // Test language type mapping
        assert WasmType.fromLanguageType("integer") == WasmType.I32;
        assert WasmType.fromLanguageType("real") == WasmType.F64;
        assert WasmType.fromLanguageType("boolean") == WasmType.I32;
        System.out.println("✓ Language type mapping correct");
        
        try {
            WasmType.fromLanguageType("unknown");
            assert false : "Should throw exception";
        } catch (CodeGenException e) {
            System.out.println("✓ Unknown type throws exception");
        }
    }

    // ========== OPERATOR TESTS ==========

    public static void testWasmOperator() {
        System.out.println("\n=== Testing WasmOperator ===");
        
        // Test binary operators
        assert WasmOperator.getBinaryOp("+", "i32").equals("i32.add");
        assert WasmOperator.getBinaryOp("+", "f64").equals("f64.add");
        assert WasmOperator.getBinaryOp("*", "i32").equals("i32.mul");
        assert WasmOperator.getBinaryOp("/", "f64").equals("f64.div");
        System.out.println("✓ Binary operators mapped correctly");
        
        // Test comparison operators
        assert WasmOperator.getBinaryOp("<", "i32").equals("i32.lt_s");
        assert WasmOperator.getBinaryOp(">=", "f64").equals("f64.ge");
        System.out.println("✓ Comparison operators mapped correctly");
        
        // Test logical operators
        assert WasmOperator.getBinaryOp("and", "i32").equals("i32.and");
        assert WasmOperator.getBinaryOp("or", "i32").equals("i32.or");
        System.out.println("✓ Logical operators mapped correctly");
        
        // Test unary operators
        String unaryMinus = WasmOperator.getUnaryOp("-", "f64");
        assert unaryMinus.equals("f64.neg");
        System.out.println("✓ Unary operators mapped correctly");
        
        // Test operator classification
        assert WasmOperator.isComparisonOp(">");
        assert WasmOperator.isLogicalOp("and");
        assert WasmOperator.isArithmeticOp("+");
        System.out.println("✓ Operator classification correct");
    }

    // ========== CODE GENERATION UTILITIES TESTS ==========

    public static void testCodeGenUtils() {
        System.out.println("\n=== Testing CodeGenUtils ===");
        
        // Test type checking
        assert CodeGenUtils.isNumericType("integer");
        assert CodeGenUtils.isNumericType("real");
        assert !CodeGenUtils.isNumericType("boolean");
        System.out.println("✓ Type checking utilities work");
        
        // Test WASM type conversion
        assert CodeGenUtils.toWasmType("integer").equals("i32");
        assert CodeGenUtils.toWasmType("real").equals("f64");
        assert CodeGenUtils.toWasmType("boolean").equals("i32");
        System.out.println("✓ Language to WASM type conversion works");
        
        // Test identifier validation
        assert CodeGenUtils.isValidIdentifier("myVar");
        assert CodeGenUtils.isValidIdentifier("_var123");
        assert !CodeGenUtils.isValidIdentifier("123var");
        assert !CodeGenUtils.isValidIdentifier("");
        System.out.println("✓ Identifier validation works");
        
        // Test label generation
        String label1 = CodeGenUtils.generateLabel("loop");
        String label2 = CodeGenUtils.generateLabel("loop");
        assert !label1.equals(label2) : "Labels should be unique";
        System.out.println("✓ Label generation creates unique labels");
    }

    // ========== SYMBOL TABLE TESTS ==========

    public static void testCodeGenSymbolTable() {
        System.out.println("\n=== Testing CodeGenSymbolTable ===");
        
        CodeGenSymbolTable table = new CodeGenSymbolTable();
        
        // Test local variable declaration
        table.declareLocal("x", "integer");
        SymbolInfo xInfo = table.lookup("x");
        assert xInfo != null;
        assert xInfo.getName().equals("x");
        assert xInfo.getType().equals("integer");
        System.out.println("✓ Local variable declaration works");
        
        // Test global variable
        table.declareGlobal("counter", "integer");
        SymbolInfo counterInfo = table.lookup("counter");
        assert counterInfo != null;
        assert counterInfo.getKind() == SymbolInfo.SymbolKind.GLOBAL;
        System.out.println("✓ Global variable declaration works");
        
        // Test parameter
        table.declareParameter("n", "integer");
        SymbolInfo nInfo = table.lookup("n");
        assert nInfo.isParameter();
        System.out.println("✓ Parameter declaration works");
        
        // Test scope
        table.enterScope();
        table.declareLocal("y", "real");
        SymbolInfo yInfo = table.lookup("y");
        assert yInfo != null;
        table.exitScope();
        
        // Should not find y after scope exit
        yInfo = table.lookup("y");
        assert yInfo == null : "y should not be visible outside scope";
        System.out.println("✓ Scope management works");
        
        // x should still be found (parent scope)
        xInfo = table.lookup("x");
        assert xInfo != null;
        System.out.println("✓ Parent scope lookups work");
    }

    // ========== MEMORY LAYOUT TESTS ==========

    public static void testMemoryLayout() {
        System.out.println("\n=== Testing MemoryLayout ===");
        
        MemoryLayout layout = new MemoryLayout();
        
        // Test type sizes
        assert MemoryLayout.getTypeSize("integer") == 4;
        assert MemoryLayout.getTypeSize("real") == 8;
        assert MemoryLayout.getTypeSize("boolean") == 4;
        System.out.println("✓ Type size calculation correct");
        
        // Test record layout
        MemoryLayout.RecordLayout point = layout.registerRecord("Point");
        point.addField("x", "real");
        point.addField("y", "real");
        assert point.getTotalSize() == 16;
        assert point.getFieldOffset("x") == 0;
        assert point.getFieldOffset("y") == 8;
        System.out.println("✓ Record layout calculation correct");
        
        // Test array allocation
        int arrayPtr1 = layout.allocateArray(10, "integer");
        int arrayPtr2 = layout.allocateArray(5, "real");
        assert arrayPtr1 < arrayPtr2 : "Second allocation should have higher address";
        System.out.println("✓ Array allocation and addresses correct");
        
        // Test record allocation
        int recordPtr = layout.allocateRecord("Point");
        assert recordPtr > arrayPtr2 : "Record should be after arrays";
        System.out.println("✓ Record allocation correct");
        
        // Test load/store instructions
        assert MemoryLayout.getLoadInstruction("integer").equals("i32.load");
        assert MemoryLayout.getLoadInstruction("real").equals("f64.load");
        assert MemoryLayout.getStoreInstruction("integer").equals("i32.store");
        System.out.println("✓ Load/store instructions correct");
    }

    // ========== WASM CODE GENERATOR TESTS ==========

    public static void testWasmCodeGenerator() {
        System.out.println("\n=== Testing WasmCodeGenerator ===");
        
        WasmCodeGenerator gen = new WasmCodeGenerator();
        
        // Test module generation
        String wat = gen.generate(null);
        assert wat.contains("(module");
        assert wat.contains("(memory 1)");
        assert wat.contains("(global $heap_ptr");
        System.out.println("✓ Module structure generated");
        
        // Test WAT output file writing capability
        assert gen.getWat() != null;
        System.out.println("✓ WAT output accessible");
    }

    // ========== WASM PRINTER TESTS ==========

    public static void testWasmPrinter() {
        System.out.println("\n=== Testing WasmPrinter ===");
        
        WasmPrinter printer = new WasmPrinter();
        
        // Test module structure
        printer.startModule();
        printer.declareMemory(1);
        printer.export("memory", "memory", "0");
        printer.endModule();
        
        String output = printer.toString();
        assert output.contains("(module");
        assert output.contains("(memory 1)");
        assert output.contains("(export");
        System.out.println("✓ Module printer works");
        
        // Test function structure
        WasmPrinter funcPrinter = new WasmPrinter();
        funcPrinter.startFunction("test", "(param $x i32)", "i32");
        funcPrinter.instruction("local.get $x");
        funcPrinter.instruction("i32.const 1");
        funcPrinter.instruction("i32.add");
        funcPrinter.endFunction();
        
        String funcOutput = funcPrinter.toString();
        assert funcOutput.contains("(func $test");
        assert funcOutput.contains("(param $x i32)");
        assert funcOutput.contains("(result i32)");
        System.out.println("✓ Function printer works");
        
        // Test indentation
        WasmPrinter indentPrinter = new WasmPrinter();
        indentPrinter.startBlock("test");
        indentPrinter.instruction("i32.const 1");
        indentPrinter.endBlock();
        String indentOutput = indentPrinter.toString();
        assert indentOutput.contains("  ") : "Should have indentation";
        System.out.println("✓ Indentation works");
    }

    // ========== MAIN TEST RUNNER ==========

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   CODE GENERATOR UNIT TESTS            ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        try {
            testWasmType();
            testWasmOperator();
            testCodeGenUtils();
            testCodeGenSymbolTable();
            testMemoryLayout();
            testWasmCodeGenerator();
            testWasmPrinter();
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║   ✅ ALL TESTS PASSED!                 ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("\n❌ TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

