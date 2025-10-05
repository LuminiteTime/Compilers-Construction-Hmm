import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import compiler.parser.Program;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser Demo - runs all test cases from README.md
 */
public class ParserDemo {
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("                  COMPILER PARSER DEMO");
        System.out.println("=".repeat(80));
        System.out.println("Testing all 10 test cases from the specification...\n");

        List<TestCase> testCases = createTestCases();

        int passed = 0;
        int failed = 0;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase test = testCases.get(i);
            System.out.println("-".repeat(60));
            System.out.println("TEST " + (i + 1) + ": " + test.name);
            System.out.println("-".repeat(60));
            System.out.println("Code:");
            System.out.println(test.code.trim());
            System.out.println("\nResult:");

            boolean success = runTest(test);
            if (success) {
                passed++;
                System.out.println("✅ PASSED");
            } else {
                failed++;
                System.out.println("❌ FAILED");
            }
            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.println("SUMMARY: " + passed + " passed, " + failed + " failed");
        System.out.println("=".repeat(80));
    }

    private static boolean runTest(TestCase test) {
        try {
            Lexer lexer = new Lexer(new StringReader(test.code));
            Parser parser = new Parser(lexer);
            Program program = parser.parse();

            if (test.shouldFail) {
                System.out.println("ERROR: Expected this test to fail, but it passed!");
                return false;
            }

            System.out.println("Successfully parsed!");
            System.out.println("AST Structure:");
            System.out.println(program.toString());
            return true;

        } catch (ParserException | LexerException e) {
            if (test.shouldFail) {
                System.out.println("Expected error: " + e.getMessage());
                return true;
            } else {
                System.out.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static List<TestCase> createTestCases() {
        List<TestCase> tests = new ArrayList<>();

        // Test 1: Basic Variable Declarations
        tests.add(new TestCase("Basic Variable Declarations", """
            var x: integer is 42;
            var y: real is 3.14;
            var flag: boolean is true;
            var name is "test";
            """, false));

        // Test 2: Array Declaration and Access
        tests.add(new TestCase("Array Declaration and Access", """
            var numbers: array[5] integer;
            numbers[1] := 10;
            numbers[2] := 20;
            var sum: integer is numbers[1] + numbers[2];
            """, false));

        // Test 3: Record Type Definition and Usage
        tests.add(new TestCase("Record Type Definition and Usage", """
            type Point is record
                var x: real;
                var y: real;
            end

            var p1: Point;
            p1.x := 1.5;
            p1.y := 2.7;
            """, false));

        // Test 4: While Loop with Boolean Expression
        tests.add(new TestCase("While Loop with Boolean Expression", """
            var counter: integer is 10;
            while counter > 0 loop
                print counter;
                counter := counter - 1;
            end
            """, false));

        // Test 5: For Loop with Range
        tests.add(new TestCase("For Loop with Range", """
            for i in 1..10 loop
                print i * i;
            end

            for j in 10..1 reverse loop
                print j;
            end
            """, false));

        // Test 6: Function Declaration and Call
        tests.add(new TestCase("Function Declaration and Call", """
            routine factorial(n: integer): integer is
                if n <= 1 then
                    return 1;
                else
                    return n * factorial(n - 1);
                end
            end

            var result: integer is factorial(5);
            """, false));

        // Test 7: Type Conversion Assignment
        tests.add(new TestCase("Type Conversion Assignment", """
            var i: integer is 42;
            var r: real is i;
            var b: boolean is 1;
            var converted: integer is true;
            """, false));

        // Test 8: Type Assignment (Note: Semantic checking would be done later)
        tests.add(new TestCase("Type Assignment - Semantic validation occurs later", """
            var flag: boolean is 3.14;
            """, false));

        // Test 9: Complex Expression with Operator Precedence
        tests.add(new TestCase("Complex Expression with Operator Precedence", """
            var result: integer is 2 + 3 * 4 - 1;
            var comparison: boolean is (result > 10) and not (result = 15);
            """, false));

        // Test 10: Array Iteration and Record Array
        tests.add(new TestCase("Array Iteration and Record Array", """
            type Student is record
                var id: integer;
                var grade: real;
            end

            var students: array[3] Student;
            students[1].id := 101;
            students[1].grade := 85.5;

            for student in students loop
                print student.id, student.grade;
            end
            """, false));

        return tests;
    }

    private static class TestCase {
        final String name;
        final String code;
        final boolean shouldFail;

        TestCase(String name, String code, boolean shouldFail) {
            this.name = name;
            this.code = code;
            this.shouldFail = shouldFail;
        }
    }
}
