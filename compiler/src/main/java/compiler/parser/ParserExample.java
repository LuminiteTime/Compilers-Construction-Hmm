package compiler.parser;

import java.io.StringReader;
import java.util.Scanner;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;

/**
 * Interactive example demonstrating the parser usage.
 * Run this to see the parser in action with different inputs.
 */
public class ParserExample {

    public static void main(String[] args) {
        System.out.println("Parser Demonstration");
        System.out.println("===================");
        System.out.println();
        System.out.println("Enter source code to parse (or 'quit' to exit):");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            try {
                Lexer lexer = new Lexer(new StringReader(input));
                Parser parser = new Parser(lexer);
                ProgramNode ast = parser.parseProgram();

                System.out.println("✓ Parsing successful!");
                System.out.println("AST:");
                System.out.println(ast.toString());
                System.out.println();

            } catch (ParserException e) {
                System.err.printf("✗ Parse error at line %d, column %d: %s%n",
                                e.getLine(), e.getColumn(), e.getMessage());
                System.out.println();
            } catch (LexerException e) {
                System.err.printf("✗ Lexical error at line %d, column %d: %s%n",
                                e.getLine(), e.getColumn(), e.getMessage());
                System.out.println();
            } catch (Exception e) {
                System.err.println("✗ Unexpected error: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }

        System.out.println("Goodbye!");
    }
}
