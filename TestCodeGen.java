import compiler.codegen.*;

public class TestCodeGen {
    public static void main(String[] args) {
        try {
            testArrayCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testArrayCase() throws Exception {
        // Create a simple WASM generator
        WasmCodeGenerator generator = new WasmCodeGenerator();

        // Start module
        generator.emit("(module");

        // Add WASI imports manually
        generator.emit("  (import \"wasi_snapshot_preview1\" \"fd_write\"");
        generator.emit("    (func $fd_write (param i32 i32 i32 i32) (result i32)))");

        // Add memory
        generator.emitMemory();

        // Add heap pointer
        generator.emitHeapPtr();

        // Add fixed print buffers
        generator.emit("(global $print_buffer i32 (i32.const 0x1000))");
        generator.emit("(global $iovec_buffer i32 (i32.const 0x1010))");

        // Add alloc function
        generator.emitAllocFunction();

        // Add print functions
        generator.emitPrintFunctionsFixed();

        // Simulate: var numbers: array[5] integer;
        generator.emit("(func $_start (local $numbers i32) (local $sum i32)");
        generator.emit("  call $init_print_buffer");

        // Allocate memory for array: 5 elements * 4 bytes + 4 bytes for size = 24 bytes
        generator.emit("  i32.const 24");
        generator.emit("  call $alloc");
        generator.emit("  local.set $numbers");

        // Store array size (5) at the beginning
        generator.emit("  local.get $numbers");
        generator.emit("  i32.const 5");
        generator.emit("  i32.store");

        // numbers[1] := 10;
        generator.emit("  local.get $numbers");
        generator.emit("  i32.const 0"); // 0-based index for element 1 (after size field)
        generator.emit("  i32.const 4"); // skip size field
        generator.emit("  i32.add");
        generator.emit("  i32.const 10");
        generator.emit("  i32.store");

        // numbers[2] := 20;
        generator.emit("  local.get $numbers");
        generator.emit("  i32.const 1"); // 0-based index for element 2
        generator.emit("  i32.const 4"); // skip size field
        generator.emit("  i32.add");
        generator.emit("  i32.const 20");
        generator.emit("  i32.store");

        // var sum: integer is numbers[1] + numbers[2];

        // Load numbers[1]
        generator.emit("  local.get $numbers");
        generator.emit("  i32.const 0"); // 0-based index for element 1
        generator.emit("  i32.const 4"); // skip size field
        generator.emit("  i32.add");
        generator.emit("  i32.load");

        // Load numbers[2]
        generator.emit("  local.get $numbers");
        generator.emit("  i32.const 1"); // 0-based index for element 2
        generator.emit("  i32.const 4"); // skip size field
        generator.emit("  i32.add");
        generator.emit("  i32.load");

        // Add them
        generator.emit("  i32.add");
        generator.emit("  local.set $sum");

        // Print a fixed value for testing
        generator.emit("  i32.const 5");
        generator.emit("  call $print_int");

        generator.emit(")");
        generator.emit("(export \"_start\" (func $_start))");
        generator.emit(")");

        String wat = generator.getOutput();
        System.out.println("Generated WASM for array test:");
        System.out.println(wat);

        // Write to file
        java.nio.file.Files.writeString(
            java.nio.file.Paths.get("array_test.wat"),
            wat
        );
        System.out.println("WASM written to array_test.wat");
    }
}