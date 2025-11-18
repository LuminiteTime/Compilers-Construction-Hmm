package compiler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * End-to-End Compiler Testing
 * Tests the complete compilation pipeline from source to WASM
 */
public class EndToEndTest {

    private static int passed = 0;
    private static int failed = 0;
    private List<String> testCases;

    public EndToEndTest() {
        this.testCases = new ArrayList<>();
    }

    /**
     * Add test case file
     */
    public void addTestCase(String filePath) {
        testCases.add(filePath);
    }

    /**
     * Test a single file
     */
    private boolean testFile(String filePath) {
        try {
            String filename = new File(filePath).getName();
            System.out.print("Testing " + filename + "... ");

            // Create temporary output file
            String outputFile = "/tmp/test_" + filename.replace(".i", ".wat");

            // Run the compiler
            Compiler.main(new String[]{filePath, "-o", outputFile});

            // Check if output file was created and is not empty
            java.io.File output = new java.io.File(outputFile);
            if (output.exists() && output.length() > 0) {
                System.out.println("✓ PASSED");
                passed++;

                // Clean up
                output.delete();
                return true;
            } else {
                System.out.println("❌ FAILED (no output generated)");
                failed++;
                return false;
            }

        } catch (Exception e) {
            System.out.println("❌ FAILED (" + e.getMessage() + ")");
            failed++;
            return false;
        }
    }

    /**
     * Run all tests
     */
    public void runAllTests() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   END-TO-END COMPILATION TESTS         ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        for (String testCase : testCases) {
            testFile(testCase);
        }

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   TEST RESULTS                         ║");
        System.out.println("║   Passed: " + String.format("%-24d", passed) + "║");
        System.out.println("║   Failed: " + String.format("%-24d", failed) + "║");
        System.out.println("║   Total:  " + String.format("%-24d", passed + failed) + "║");
        System.out.println("╚════════════════════════════════════════╝\n");

        if (failed == 0) {
            System.out.println("✅ ALL TESTS PASSED!");
            System.exit(0);
        } else {
            System.out.println("❌ SOME TESTS FAILED!");
            System.exit(1);
        }
    }

    /**
     * Main test runner
     */
    public static void main(String[] args) {
        // Load the native JNI library
        try {
            String libPath = System.getProperty("user.dir") + "/compiler/src/main/cpp/parser/libparser.so";
            System.load(libPath);
            System.out.println("✓ Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("✗ Failed to load native library: " + e.getMessage());
            System.exit(1);
        }

        EndToEndTest tester = new EndToEndTest();

        // Parser test cases - basics
        tester.addTestCase("tests/cases/parser/basics/print_single.i");
        tester.addTestCase("tests/cases/parser/basics/var_init_integer_literal.i");
        tester.addTestCase("tests/cases/parser/basics/declarations_mixed_types.i");

        // Parser test cases - arrays
        tester.addTestCase("tests/cases/parser/arrays/array_declaration_indexing_sum.i");

        // Parser test cases - records
        tester.addTestCase("tests/cases/parser/records/record_declaration_field_assign.i");

        // Parser test cases - routines
        tester.addTestCase("tests/cases/parser/routines/routine_definition_and_call.i");

        // Parser test cases - control flow
        tester.addTestCase("tests/cases/parser/control_flow/while/while_countdown_print.i");
        tester.addTestCase("tests/cases/parser/control_flow/for/for_ranges_and_reverse.i");

        // Parser test cases - precedence
        tester.addTestCase("tests/cases/parser/precedence/plus_times.i");
        tester.addTestCase("tests/cases/parser/precedence/expr_with_comparison_and_not.i");

        // Analyzer test cases
        tester.addTestCase("tests/cases/analyzer/print/print_multiple.i");
        tester.addTestCase("tests/cases/analyzer/arrays/array_checks.i");
        tester.addTestCase("tests/cases/analyzer/records/record_field.i");

        System.out.println("Testing " + tester.testCases.size() + " test cases...");
        tester.runAllTests();
    }
}

