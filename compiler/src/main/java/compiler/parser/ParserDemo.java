package compiler.parser;

import java.io.StringReader;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;

/**
 * Demonstration of how to use the Parser class.
 * This class shows basic usage patterns and error handling.
 */
public class ParserDemo {

    public static void main(String[] args) {
        // Example 1: Simple variable declaration
        parseAndPrint("var x: integer is 42;");

        // Example 2: Type declaration with record
        parseAndPrint("""
            type Point is record
                var x: real;
                var y: real;
            end
            """);

        // Example 3: Routine declaration
        parseAndPrint("""
            routine factorial(n: integer): integer is
                if n <= 1 then
                    return 1;
                else
                    return n * factorial(n - 1);
                end
            end
            """);

        // Example 4: Complex program
        parseAndPrint("""
            var counter: integer is 10;
            while counter > 0 loop
                print counter;
                counter := counter - 1;
            end
            """);

        // Example 5: Error handling
        System.out.println("=== Error Example ===");
        try {
            parseProgram("var x: invalid_type;");
        } catch (ParserException e) {
            System.err.printf("Parse error at line %d, column %d: %s%n",
                             e.getLine(), e.getColumn(), e.getMessage());
        } catch (LexerException e) {
            System.err.printf("Lexical error at line %d, column %d: %s%n",
                             e.getLine(), e.getColumn(), e.getMessage());
        }
    }

    /**
     * Parse a program string and print the AST.
     */
    public static void parseAndPrint(String sourceCode) {
        System.out.println("=== Parsing ===");
        System.out.println(sourceCode.trim());

        try {
            ProgramNode ast = parseProgram(sourceCode);
            System.out.println("=== AST ===");
            System.out.println(ast.toString());
        } catch (ParserException e) {
            System.err.printf("Parse error: %s%n", e.getMessage());
        } catch (LexerException e) {
            System.err.printf("Lexical error: %s%n", e.getMessage());
        }

        System.out.println();
    }

    /**
     * Parse a program from source code string.
     */
    public static ProgramNode parseProgram(String sourceCode) throws LexerException, ParserException {
        Lexer lexer = new Lexer(new StringReader(sourceCode));
        Parser parser = new Parser(lexer);
        return parser.parseProgram();
    }

    /**
     * Example of AST traversal - count different node types.
     */
    public static void analyzeAST(ProgramNode program) {
        NodeCounter counter = new NodeCounter();
        counter.visit(program);
        System.out.println("AST Analysis:");
        System.out.println("Declarations: " + counter.declarationCount);
        System.out.println("Statements: " + counter.statementCount);
        System.out.println("Expressions: " + counter.expressionCount);
    }

    /**
     * Simple AST visitor that counts node types.
     */
    static class NodeCounter {
        int declarationCount = 0;
        int statementCount = 0;
        int expressionCount = 0;

        public void visit(AstNode node) {
            if (node instanceof VariableDeclarationNode || node instanceof TypeDeclarationNode ||
                node instanceof RoutineDeclarationNode) {
                declarationCount++;
            } else if (node instanceof StatementNode) {
                statementCount++;
            } else if (node instanceof ExpressionNode) {
                expressionCount++;
            }

            // Visit children (simplified - would need to be more comprehensive)
            if (node instanceof ProgramNode program) {
                for (AstNode child : program.getDeclarations()) {
                    visit(child);
                }
            // Note: For a full visitor pattern, you'd need to handle ExpressionNode separately
            // since it's an interface, not a class extending AstNode
            }
            // Add more cases as needed...
        }
    }
}
