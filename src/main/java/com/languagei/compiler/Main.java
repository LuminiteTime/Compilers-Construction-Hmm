package com.languagei.compiler;

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
                        System.err.println("Usage: run <source.i> [-o output.wasm]");
                        return;
                    }
                    sourceFile = args[1];
                    String wasmFile = "output.wasm";
                    
                    for (int i = 2; i < args.length; i++) {
                        if ("-o".equals(args[i]) && i + 1 < args.length) {
                            wasmFile = args[++i];
                        }
                    }
                    
                    compiler = new Compiler();
                    String watFile = wasmFile.replace(".wasm", ".wat");
                    compiler.compile(sourceFile, watFile);
                    System.out.println("✓ Compiled to WAT");
                    break;

                case "ast":
                    if (args.length < 2) {
                        System.err.println("Usage: ast <source.i>");
                        return;
                    }
                    sourceFile = args[1];
                    compiler = new Compiler();
                    var ast = compiler.compileToAST(sourceFile);
                    System.out.println("AST: " + ast);
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
        System.out.println("  java -jar compiler-i.jar run <source.i> [-o output.wasm]");
        System.out.println("  java -jar compiler-i.jar ast <source.i>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  compile  Compile Language I source to WebAssembly Text format");
        System.out.println("  run      Compile and run a Language I program");
        System.out.println("  ast      Display the Abstract Syntax Tree");
    }
}
