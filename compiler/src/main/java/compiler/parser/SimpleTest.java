package compiler.parser;

import java.io.StringReader;

import compiler.lexer.Lexer;

public class SimpleTest {
    public static void main(String[] args) {
        testParsing("var x: integer;", "Simple variable");
        testParsing("type Point is record var x: integer; var y: integer; end", "Record type");
        testParsing("var arr: array[10] integer;", "Array type");
        testParsing("x := 5;", "Assignment");
    }

    private static void testParsing(String input, String description) {
        try {
            System.out.println("=== Testing " + description + " ===");
            System.out.println("Input: " + input);

            Lexer lexer = new Lexer(new StringReader(input));
            Parser parser = new Parser(lexer);

            ProgramNode program = parser.parseProgram();
            System.out.println("Success! AST: " + program.toString());
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in " + description + ": " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }
}
