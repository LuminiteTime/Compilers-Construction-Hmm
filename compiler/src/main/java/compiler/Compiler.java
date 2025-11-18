package compiler;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import compiler.codegen.CodeGenException;
import compiler.codegen.CppASTBridge;
import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class Compiler {

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Usage: java compiler.Compiler <source-file> [-o <output-file>]");
            System.exit(1);
        }

        // Load the native JNI library
        try {
            String libPath = System.getProperty("user.dir") + "/compiler/src/main/cpp/parser/libparser.so";
            System.load(libPath);
            System.out.println("✓ Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("✗ Failed to load native library: " + e.getMessage());
            System.exit(1);
        }

        String filename = args[0];
        String outputFile = "output.wat";

        // Parse command line arguments
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-o") && i + 1 < args.length) {
                outputFile = args[++i];
            }
        }

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

            // Parse the input (C++ parser integration)
            boolean parseResult = lexer.parseInput(sourceCode);

            if (!parseResult) {
                System.out.println("✗ Parsing failed!");
                System.exit(1);
            }

            System.out.println("✓ Parsing successful!");

            // Code generation using Java code generator
            System.out.println("Generating WebAssembly code...");
            CppASTBridge cppBridge = new CppASTBridge(0, null);
            long astPointer = cppBridge.getASTPointer();

            if (astPointer == 0) {
                System.err.println("✗ Failed to get AST pointer from C++ parser!");
                System.exit(1);
            }

            // Use Java code generator instead of C++
            String wat = cppBridge.generate();

            // Write output
            Files.writeString(Paths.get(outputFile), wat);
            System.out.println("✓ Code generation successful!");
            System.out.println("Output written to: " + outputFile);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (LexerException e) {
            System.err.println("Lexer error: " + e.getMessage());
            System.exit(1);
        } catch (CodeGenException e) {
            System.err.println("Code generation error: " + e.getMessage());
            System.exit(1);
        }
    }
}
