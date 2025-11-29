package com.languagei.compiler;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.lexer.Lexer;
import com.languagei.compiler.parser.Parser;
import com.languagei.compiler.semantic.SemanticAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticTest {

    private ProgramNode parseCode(String code) {
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        return parser.parse();
    }

    @Test
    public void testVariableDeclarationWithType() {
        String code = "var x : integer is 42";
        ProgramNode ast = parseCode(code);
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(ast);
        
        assertFalse(analyzer.hasErrors());
    }

    @Test
    public void testUndefinedVariable() {
        String code = "var x : integer is y + 1";
        ProgramNode ast = parseCode(code);
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(ast);
        
        assertTrue(analyzer.hasErrors());
    }

    @Test
    public void testTypeMismatch() {
        String code = "var x : boolean is 42";
        ProgramNode ast = parseCode(code);
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(ast);
        
        assertTrue(analyzer.hasErrors());
    }

    @Test
    public void testComplexExpression() {
        String code = "var result is 2 + 3 * 4";
        ProgramNode ast = parseCode(code);
        
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(ast);
        
        assertFalse(analyzer.hasErrors());
    }
}

