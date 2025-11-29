package com.languagei.compiler;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.lexer.Lexer;
import com.languagei.compiler.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testSimpleVariableDeclaration() {
        String code = "var x : integer is 42";
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        ProgramNode program = parser.parse();
        
        assertEquals(1, program.getDeclarations().size());
        ASTNode decl = program.getDeclarations().get(0);
        assertTrue(decl instanceof VariableDeclarationNode);
        
        VariableDeclarationNode varDecl = (VariableDeclarationNode) decl;
        assertEquals("x", varDecl.getName());
        assertNotNull(varDecl.getType());
        assertNotNull(varDecl.getInitializer());
    }

    @Test
    public void testTypeInference() {
        String code = "var y is 3.14";
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        ProgramNode program = parser.parse();
        
        VariableDeclarationNode varDecl = (VariableDeclarationNode) program.getDeclarations().get(0);
        assertEquals("y", varDecl.getName());
        assertNull(varDecl.getType()); // Type should be inferred
        assertNotNull(varDecl.getInitializer());
    }

    @Test
    public void testBinaryExpression() {
        String code = "var z is 5 + 3";
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        ProgramNode program = parser.parse();
        
        VariableDeclarationNode varDecl = (VariableDeclarationNode) program.getDeclarations().get(0);
        assertTrue(varDecl.getInitializer() instanceof BinaryExpressionNode);
        
        BinaryExpressionNode binExpr = (BinaryExpressionNode) varDecl.getInitializer();
        assertEquals(BinaryExpressionNode.Operator.PLUS, binExpr.getOperator());
    }

    @Test
    public void testIfStatement() {
        String code = "if 5 > 3 then\n var x : integer is 1\nend";
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        ProgramNode program = parser.parse();
        
        ASTNode stmt = program.getDeclarations().get(0);
        assertTrue(stmt instanceof IfStatementNode);
    }

    @Test
    public void testRoutineDeclaration() {
        String code = "routine add(a : integer, b : integer) : integer\nis\n  a + b\nend";
        Lexer lexer = new Lexer(code, "test.i");
        Parser parser = new Parser(lexer);
        ProgramNode program = parser.parse();
        
        ASTNode decl = program.getDeclarations().get(0);
        assertTrue(decl instanceof RoutineDeclarationNode);
        
        RoutineDeclarationNode routine = (RoutineDeclarationNode) decl;
        assertEquals("add", routine.getName());
        assertEquals(2, routine.getParameters().size());
        assertNotNull(routine.getReturnType());
        assertNotNull(routine.getBody());
    }
}

