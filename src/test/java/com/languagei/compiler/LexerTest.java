package com.languagei.compiler;

import com.languagei.compiler.lexer.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LexerTest {

    @Test
    public void testKeywordRecognition() {
        Lexer lexer = new Lexer("var type routine", "test.i");
        
        Token t1 = lexer.nextToken();
        assertEquals(TokenType.VAR, t1.getType());
        assertEquals("var", t1.getLexeme());
        
        Token t2 = lexer.nextToken();
        assertEquals(TokenType.TYPE, t2.getType());
        
        Token t3 = lexer.nextToken();
        assertEquals(TokenType.ROUTINE, t3.getType());
    }

    @Test
    public void testIdentifierRecognition() {
        Lexer lexer = new Lexer("myVariable x123", "test.i");
        
        Token t1 = lexer.nextToken();
        assertEquals(TokenType.IDENTIFIER, t1.getType());
        assertEquals("myVariable", t1.getLexeme());
        
        Token t2 = lexer.nextToken();
        assertEquals(TokenType.IDENTIFIER, t2.getType());
        assertEquals("x123", t2.getLexeme());
    }

    @Test
    public void testNumberLiterals() {
        Lexer lexer = new Lexer("42 3.14 0", "test.i");
        
        Token t1 = lexer.nextToken();
        assertEquals(TokenType.INTEGER_LITERAL, t1.getType());
        assertEquals(42L, t1.getLiteral());
        
        Token t2 = lexer.nextToken();
        assertEquals(TokenType.REAL_LITERAL, t2.getType());
        assertEquals(3.14, t2.getLiteral());
        
        Token t3 = lexer.nextToken();
        assertEquals(TokenType.INTEGER_LITERAL, t3.getType());
    }

    @Test
    public void testOperators() {
        Lexer lexer = new Lexer(":= + - * /", "test.i");
        
        Token t1 = lexer.nextToken();
        assertEquals(TokenType.ASSIGN, t1.getType());
        
        Token t2 = lexer.nextToken();
        assertEquals(TokenType.PLUS, t2.getType());
    }

    @Test
    public void testCommentSkipping() {
        Lexer lexer = new Lexer("var x // comment\nvar y", "test.i");
        
        assertEquals(TokenType.VAR, lexer.nextToken().getType());
        assertEquals(TokenType.IDENTIFIER, lexer.nextToken().getType());
        assertEquals(TokenType.VAR, lexer.nextToken().getType());
        assertEquals(TokenType.IDENTIFIER, lexer.nextToken().getType());
    }

    @Test
    public void testEOF() {
        Lexer lexer = new Lexer("var", "test.i");
        
        lexer.nextToken(); // var
        Token eof = lexer.nextToken();
        assertEquals(TokenType.EOF, eof.getType());
    }
}

