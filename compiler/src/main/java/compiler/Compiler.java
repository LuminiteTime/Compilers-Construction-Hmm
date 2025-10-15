package compiler;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class Compiler {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java compiler.Compiler <source-file>");
            System.exit(1);
        }

        String filename = args[0];

        try {
            // Read source code
            String sourceCode = Files.readString(Paths.get(filename));
            System.out.println("Compiling file: " + filename);

            // Create lexer
            Lexer lexer = new Lexer(new StringReader(sourceCode));

            // Initialize parser
            lexer.initializeParser();

            // Tokenize and collect all tokens
            List<Token> tokens = new ArrayList<>();
            Token token;
            while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
                tokens.add(token);
            }
            tokens.add(token); // Add EOF

            System.out.println("Tokenization completed. Found " + tokens.size() + " tokens.");

            // For now, just parse the input directly
            boolean parseResult = lexer.parseInput(sourceCode);

            if (parseResult) {
                System.out.println("✓ Compilation successful!");
            } else {
                System.out.println("✗ Compilation failed!");
                System.exit(1);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (LexerException e) {
            System.err.println("Lexer error: " + e.getMessage());
            System.exit(1);
        }
    }
}
