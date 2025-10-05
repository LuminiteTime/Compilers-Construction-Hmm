package compiler.hmm;

import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import compiler.parser.Program;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the recursive descent parser.
 * Tests all 8 test cases from the specification to ensure they parse without syntax errors.
 */
public class TestParser {

    /**
     * Test 1: Variable Declarations
     * var x: integer is 42;
     * var y: real is 3.14;
     * var flag: boolean is true;
     * var name is "test";
     */
    @Test
    public void testVariableDeclarations() throws Exception {
        String code = """
            var x: integer is 42;
            var y: real is 3.14;
            var flag: boolean is true;
            var name is "test";
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(4, program.nodes.size());
        System.out.println("Test 1 PASSED: Variable declarations parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 2: Arrays & Data Structures
     * var numbers: array[5] integer;
     * numbers[1] := 10;
     * numbers[2] := 20;
     * var sum: integer is numbers[1] + numbers[2];
     */
    @Test
    public void testArraysAndDataStructures() throws Exception {
        String code = """
            var numbers: array[5] integer;
            numbers[1] := 10;
            numbers[2] := 20;
            var sum: integer is numbers[1] + numbers[2];
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(4, program.nodes.size());
        System.out.println("Test 2 PASSED: Arrays and data structures parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 3: Record Types
     * type Point is record
     *     var x: real;
     *     var y: real;
     * end
     * var p1: Point;
     * p1.x := 1.5;
     * p1.y := 2.7;
     */
    @Test
    public void testRecordTypes() throws Exception {
        String code = """
            type Point is record
                var x: real;
                var y: real;
            end
            var p1: Point;
            p1.x := 1.5;
            p1.y := 2.7;
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(4, program.nodes.size());
        System.out.println("Test 3 PASSED: Record types parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 4: While Loops
     * var counter: integer is 10;
     * while counter > 0 loop
     *     print counter;
     *     counter := counter - 1;
     * end
     */
    @Test
    public void testWhileLoops() throws Exception {
        String code = """
            var counter: integer is 10;
            while counter > 0 loop
                print counter;
                counter := counter - 1;
            end
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(2, program.nodes.size());
        System.out.println("Test 4 PASSED: While loops parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 5: For Loops
     * for i in 1..10 loop
     *     print i * i;
     * end
     * for j in 10..1 reverse loop
     *     print j;
     * end
     */
    @Test
    public void testForLoops() throws Exception {
        String code = """
            for i in 1..10 loop
                print i * i;
            end
            for j in 10..1 reverse loop
                print j;
            end
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(2, program.nodes.size());
        System.out.println("Test 5 PASSED: For loops parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 6: Functions & Recursion
     * routine factorial(n: integer): integer is
     *     if n <= 1 then
     *         return 1;
     *     else
     *         return n * factorial(n - 1);
     *     end
     * end
     * var result: integer is factorial(5);
     */
    @Test
    public void testFunctionsAndRecursion() throws Exception {
        String code = """
            routine factorial(n: integer): integer is
                if n <= 1 then
                    return 1;
                else
                    return n * factorial(n - 1);
                end
            end
            var result: integer is factorial(5);
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(2, program.nodes.size());
        System.out.println("Test 6 PASSED: Functions and recursion parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 7: Type Conversions
     * var i: integer is 42;
     * var r: real is i;
     * var b: boolean is 1;
     * var converted: integer is true;
     */
    @Test
    public void testTypeConversions() throws Exception {
        String code = """
            var i: integer is 42;
            var r: real is i;
            var b: boolean is 1;
            var converted: integer is true;
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(4, program.nodes.size());
        System.out.println("Test 7 PASSED: Type conversions parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Test 8: Complex Data Structures
     * type Student is record
     *     var id: integer;
     *     var grade: real;
     * end
     * var students: array[3] Student;
     * students[1].id := 101;
     * students[1].grade := 85.5;
     * for student in students loop
     *     print student.id, student.grade;
     * end
     */
    @Test
    public void testComplexDataStructures() throws Exception {
        String code = """
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
            """;

        Program program = parseProgram(code);
        assertNotNull(program);
        assertEquals(5, program.nodes.size());
        System.out.println("Test 8 PASSED: Complex data structures parsed successfully");
        System.out.println(program.toString());
    }

    /**
     * Helper method to parse a program from source code string.
     */
    private Program parseProgram(String sourceCode) throws Exception {
        Lexer lexer = new Lexer(new StringReader(sourceCode));
        Parser parser = new Parser(lexer);
        return parser.parse();
    }
}
