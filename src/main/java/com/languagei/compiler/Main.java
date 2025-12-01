package com.languagei.compiler;

import com.languagei.compiler.ast.ASTPrinter;
import com.languagei.compiler.ast.ProgramNode;
import com.languagei.compiler.lexer.Lexer;
import com.languagei.compiler.lexer.Token;
import com.languagei.compiler.lexer.TokenType;

import java.io.IOException;

/**
 * Main entry point for Language I compiler
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "compile":
                    if (args.length < 2) {
                        System.err.println("Usage: compile <source.i> [-o output.wat]");
                        return;
                    }
                    String sourceFile = args[1];
                    String outputFile = "output.wat";
                    
                    for (int i = 2; i < args.length; i++) {
                        if ("-o".equals(args[i]) && i + 1 < args.length) {
                            outputFile = args[++i];
                        }
                    }
                    
                    Compiler compiler = new Compiler();
                    compiler.compile(sourceFile, outputFile);
                    System.out.println("✓ Compilation successful!");
                    break;

                case "run":
                    if (args.length < 2) {
                        System.err.println("Usage: run <source.i> [-o output.wat]");
                        return;
                    }
                    sourceFile = args[1];
                    String watFileArg = "output.wat";

                    for (int i = 2; i < args.length; i++) {
                        if ("-o".equals(args[i]) && i + 1 < args.length) {
                            watFileArg = args[++i];
                        }
                    }

                    compiler = new Compiler();
                    String watFile = watFileArg;
                    compiler.compile(sourceFile, watFile);
                    System.out.println("\u2713 Compiled to WAT");

                    // After successful compilation, execute the generated WAT via wasmtime
                    try {
                        ProcessBuilder pb = new ProcessBuilder("wasmtime", watFile);
                        pb.inheritIO();
                        Process proc = pb.start();
                        int exitCode = proc.waitFor();
                        if (exitCode != 0) {
                            System.err.println("\u2717 wasmtime exited with code " + exitCode);
                            System.exit(exitCode);
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException("Failed to run wasmtime", e);
                    }
                    break;

                case "ast":
                    if (args.length < 2) {
                        System.err.println("Usage: ast <source.i>");
                        return;
                    }
                    sourceFile = args[1];
                    compiler = new Compiler();
                    ProgramNode ast = compiler.compileToOptimizedAST(sourceFile);
                    ASTPrinter.print(ast);
                    break;

                case "tokens":
                    if (args.length < 2) {
                        System.err.println("Usage: tokens <source.i>");
                        return;
                    }
                    sourceFile = args[1];
                    try {
                        Lexer lexer = Lexer.fromFile(sourceFile);
                        while (true) {
                            Token t = lexer.nextToken();
                            System.out.println(t);
                            if (t.getType() == TokenType.EOF) break;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to lex file " + sourceFile, e);
                    }
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    printHelp();
            }
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("Language I Compiler v1.0.0");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar compiler-i.jar compile <source.i> [-o output.wat]");
        System.out.println("  java -jar compiler-i.jar run <source.i> [-o output.wat]");
        System.out.println("  java -jar compiler-i.jar ast <source.i>");
        System.out.println("  java -jar compiler-i.jar tokens <source.i>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  compile  Compile Language I source to WebAssembly Text format");
        System.out.println("  run      Compile and run a Language I program");
        System.out.println("  ast      Display the optimized Abstract Syntax Tree");
        System.out.println("  tokens   Display the token stream produced by the lexer");
    }
}
