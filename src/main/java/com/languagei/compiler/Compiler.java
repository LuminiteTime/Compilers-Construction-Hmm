package com.languagei.compiler;

import com.languagei.compiler.ast.ProgramNode;
import com.languagei.compiler.codegen.CodeGenerator;
import com.languagei.compiler.lexer.Lexer;
import com.languagei.compiler.parser.Parser;
import com.languagei.compiler.semantic.CompilationError;
import com.languagei.compiler.semantic.ConstantFolder;
import com.languagei.compiler.semantic.DeadCodeEliminator;
import com.languagei.compiler.semantic.SemanticAnalyzer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Main compiler class orchestrating all compilation stages
 */
public class Compiler {
    private List<CompilationError> errors;
    private List<CompilationError> warnings;

    public Compiler() {
        this.errors = new java.util.ArrayList<>();
        this.warnings = new java.util.ArrayList<>();
    }

    public ProgramNode compileToAST(String sourceFile) throws IOException {
        Lexer lexer = Lexer.fromFile(sourceFile);
        Parser parser = new Parser(lexer);
        ProgramNode ast = parser.parse();
        return ast;
    }

    /**
     * Parse, run semantic analysis, and apply optimization passes (constant
     * folding and simple dead-code elimination). Returns the optimized AST
     * ready for code generation or inspection.
     */
    public ProgramNode compileToOptimizedAST(String sourceFile) throws IOException {
        ProgramNode ast = compileToAST(sourceFile);

        // Semantic analysis
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(ast);

        if (semanticAnalyzer.hasErrors()) {
            System.err.println("Compilation failed due to semantic errors:");
            for (CompilationError error : semanticAnalyzer.getErrors()) {
                System.err.println("  " + error);
            }
            throw new RuntimeException("Compilation failed due to semantic errors");
        }

        // Optimization passes on AST
        ConstantFolder constantFolder = new ConstantFolder();
        ast = constantFolder.optimize(ast);

        DeadCodeEliminator dce = new DeadCodeEliminator();
        ast = dce.optimize(ast);

        return ast;
    }

    public void compile(String sourceFile, String outputFile) throws IOException {
        // Ensure output directory exists
        java.io.File outputDir = new java.io.File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // If outputFile doesn't contain path separators, put it in output directory
        if (!outputFile.contains("/") && !outputFile.contains("\\")) {
            outputFile = "output/" + outputFile;
        }

        // Parse, analyze and optimize AST
        ProgramNode ast = compileToOptimizedAST(sourceFile);

        // Code generation from optimized AST
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            CodeGenerator codegen = new CodeGenerator(osw);
            codegen.generate(ast);
        }
    }

    public void run(String wasmFile, String[] args) throws IOException, InterruptedException {
        System.out.println("=== Stage 5: Execution ===");
        System.out.println("Running " + wasmFile);

        ProcessBuilder pb = new ProcessBuilder("wasmtime", wasmFile);
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        System.out.println("Program exited with code " + exitCode);
    }

    public List<CompilationError> getErrors() {
        return errors;
    }

    public List<CompilationError> getWarnings() {
        return warnings;
    }
}

