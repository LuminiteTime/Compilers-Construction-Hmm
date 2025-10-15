package compiler.hmm;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import compiler.parser.ProgramNode;

public class TestParser {

    private ProgramNode parse(String sourceCode) throws LexerException, ParserException {
        Lexer lexer = new Lexer(new StringReader(sourceCode));
        Parser parser = new Parser(lexer);
        return parser.parseProgram();
    }

    @Test
    public void testSimpleVariableDeclaration() throws LexerException, ParserException {
        String sourceCode = "var x: integer;";
        ProgramNode program = parse(sourceCode);
        // Basic test that parsing doesn't throw
        assertEquals(1, program.getDeclarations().size());
    }

    @Test
    public void testVariableDeclarationWithInitializer() throws LexerException, ParserException {
        String sourceCode = "var x: integer is 42;";
        ProgramNode program = parse(sourceCode);
        assertEquals(1, program.getDeclarations().size());
    }

    @Test
    public void testTypeDeclaration() throws LexerException, ParserException {
        String sourceCode = "type MyType is integer;";
        try {
            ProgramNode program = parse(sourceCode);
            assertEquals(1, program.getDeclarations().size());
        } catch (ParserException e) {
            System.out.println("ParserException: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void testArrayType() throws LexerException, ParserException {
        String sourceCode = "var arr: array[10] integer;";
        ProgramNode program = parse(sourceCode);
        assertEquals(1, program.getDeclarations().size());
    }

    @Test
    public void testRecordType() throws LexerException, ParserException {
        String sourceCode = "type Point is record var x: integer; var y: integer; end";
        ProgramNode program = parse(sourceCode);
        assertEquals(1, program.getDeclarations().size());
    }

    @Test
    public void testAssignment() throws LexerException, ParserException {
        String sourceCode = "x := 5;";
        ProgramNode program = parse(sourceCode);
        // Assignment is parsed as a statement
        assertEquals(1, program.getDeclarations().size());
    }

    @Test
    public void testExpression() throws LexerException, ParserException {
        String sourceCode = "var result: integer is 2 + 3 * 4;";
        ProgramNode program = parse(sourceCode);
        assertEquals(1, program.getDeclarations().size());
    }
}
