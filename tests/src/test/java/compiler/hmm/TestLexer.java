package compiler.hmm;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class TestLexer {

    private List<Token> tokenize(String sourceCode) throws LexerException {
        Lexer lexer = new Lexer(new StringReader(sourceCode));
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
            tokens.add(token);
        }
        tokens.add(token); // add EOF
        return tokens;
    }

    @Test
    public void testVariableDeclarations() throws LexerException {
        String sourceCode = """
            var x: integer is 42;
            var y: real is 3.14;
            var flag: boolean is true;
            var name is "test";""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 12, 1),
            new Token(TokenType.IDENTIFIER, "x", 1, 5),
            new Token(TokenType.COLON, ":", 1, 6),
            new Token(TokenType.INTEGER, "integer", 1, 8),
            new Token(TokenType.IS, "is", 1, 16),
            new Token(TokenType.INTEGER_LITERAL, "42", 1, 19),
            new Token(TokenType.SEMICOLON, ";", 1, 21),
            new Token(TokenType.VAR, "var", 2, 1),
            new Token(TokenType.IDENTIFIER, "y", 2, 5),
            new Token(TokenType.COLON, ":", 2, 6),
            new Token(TokenType.REAL, "real", 2, 8),
            new Token(TokenType.IS, "is", 2, 13),
            new Token(TokenType.REAL_LITERAL, "3.14", 2, 16),
            new Token(TokenType.SEMICOLON, ";", 2, 20),
            new Token(TokenType.VAR, "var", 3, 1),
            new Token(TokenType.IDENTIFIER, "flag", 3, 5),
            new Token(TokenType.COLON, ":", 3, 9),
            new Token(TokenType.BOOLEAN, "boolean", 3, 11),
            new Token(TokenType.IS, "is", 3, 19),
            new Token(TokenType.TRUE, "true", 3, 22),
            new Token(TokenType.SEMICOLON, ";", 3, 26),
            new Token(TokenType.VAR, "var", 4, 1),
            new Token(TokenType.IDENTIFIER, "name", 4, 5),
            new Token(TokenType.IS, "is", 4, 10),
            new Token(TokenType.STRING_LITERAL, "\"test\"", 4, 13),
            new Token(TokenType.SEMICOLON, ";", 4, 19),
            new Token(TokenType.EOF, "", 4, 20)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testArraysDataStructures() throws LexerException {
        String sourceCode = """
            var numbers: array[5] integer;
            numbers[1] := 10;
            numbers[2] := 20;
            var sum: integer is numbers[1] + numbers[2];""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "numbers", 1, 5),
            new Token(TokenType.COLON, ":", 1, 12),
            new Token(TokenType.ARRAY, "array", 1, 14),
            new Token(TokenType.LBRACKET, "[", 1, 19),
            new Token(TokenType.INTEGER_LITERAL, "5", 1, 20),
            new Token(TokenType.RBRACKET, "]", 1, 21),
            new Token(TokenType.INTEGER, "integer", 1, 23),
            new Token(TokenType.SEMICOLON, ";", 1, 30),
            new Token(TokenType.IDENTIFIER, "numbers", 2, 1),
            new Token(TokenType.LBRACKET, "[", 2, 8),
            new Token(TokenType.INTEGER_LITERAL, "1", 2, 9),
            new Token(TokenType.RBRACKET, "]", 2, 10),
            new Token(TokenType.ASSIGN, ":=", 2, 12),
            new Token(TokenType.INTEGER_LITERAL, "10", 2, 15),
            new Token(TokenType.SEMICOLON, ";", 2, 17),
            new Token(TokenType.IDENTIFIER, "numbers", 3, 1),
            new Token(TokenType.LBRACKET, "[", 3, 8),
            new Token(TokenType.INTEGER_LITERAL, "2", 3, 9),
            new Token(TokenType.RBRACKET, "]", 3, 10),
            new Token(TokenType.ASSIGN, ":=", 3, 12),
            new Token(TokenType.INTEGER_LITERAL, "20", 3, 15),
            new Token(TokenType.SEMICOLON, ";", 3, 17),
            new Token(TokenType.VAR, "var", 4, 1),
            new Token(TokenType.IDENTIFIER, "sum", 4, 5),
            new Token(TokenType.COLON, ":", 4, 8),
            new Token(TokenType.INTEGER, "integer", 4, 10),
            new Token(TokenType.IS, "is", 4, 18),
            new Token(TokenType.IDENTIFIER, "numbers", 4, 21),
            new Token(TokenType.LBRACKET, "[", 4, 28),
            new Token(TokenType.INTEGER_LITERAL, "1", 4, 29),
            new Token(TokenType.RBRACKET, "]", 4, 30),
            new Token(TokenType.PLUS, "+", 4, 32),
            new Token(TokenType.IDENTIFIER, "numbers", 4, 34),
            new Token(TokenType.LBRACKET, "[", 4, 41),
            new Token(TokenType.INTEGER_LITERAL, "2", 4, 42),
            new Token(TokenType.RBRACKET, "]", 4, 43),
            new Token(TokenType.SEMICOLON, ";", 4, 44),
            new Token(TokenType.EOF, "", 4, 45)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testRecordTypes() throws LexerException {
        String sourceCode = """
            type Point is record
                var x: real;
                var y: real;
            end
            var p1: Point;
            p1.x := 1.5;
            p1.y := 2.7;""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.TYPE, "type", 1, 1),
            new Token(TokenType.IDENTIFIER, "Point", 1, 6),
            new Token(TokenType.IS, "is", 1, 12),
            new Token(TokenType.RECORD, "record", 1, 15),
            new Token(TokenType.VAR, "var", 2, 5),
            new Token(TokenType.IDENTIFIER, "x", 2, 9),
            new Token(TokenType.COLON, ":", 2, 10),
            new Token(TokenType.REAL, "real", 2, 12),
            new Token(TokenType.SEMICOLON, ";", 2, 16),
            new Token(TokenType.VAR, "var", 3, 5),
            new Token(TokenType.IDENTIFIER, "y", 3, 9),
            new Token(TokenType.COLON, ":", 3, 10),
            new Token(TokenType.REAL, "real", 3, 12),
            new Token(TokenType.SEMICOLON, ";", 3, 16),
            new Token(TokenType.END, "end", 4, 1),
            new Token(TokenType.VAR, "var", 5, 1),
            new Token(TokenType.IDENTIFIER, "p1", 5, 5),
            new Token(TokenType.COLON, ":", 5, 7),
            new Token(TokenType.IDENTIFIER, "Point", 5, 9),
            new Token(TokenType.SEMICOLON, ";", 5, 14),
            new Token(TokenType.IDENTIFIER, "p1", 6, 1),
            new Token(TokenType.DOT, ".", 6, 3),
            new Token(TokenType.IDENTIFIER, "x", 6, 4),
            new Token(TokenType.ASSIGN, ":=", 6, 6),
            new Token(TokenType.REAL_LITERAL, "1.5", 6, 9),
            new Token(TokenType.SEMICOLON, ";", 6, 12),
            new Token(TokenType.IDENTIFIER, "p1", 7, 1),
            new Token(TokenType.DOT, ".", 7, 3),
            new Token(TokenType.IDENTIFIER, "y", 7, 4),
            new Token(TokenType.ASSIGN, ":=", 7, 6),
            new Token(TokenType.REAL_LITERAL, "2.7", 7, 9),
            new Token(TokenType.SEMICOLON, ";", 7, 12),
            new Token(TokenType.EOF, "", 7, 13)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testWhileLoops() throws LexerException {
        String sourceCode = """
            var counter: integer is 10;
            while counter > 0 loop
                print counter;
                counter := counter - 1;
            end""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "counter", 1, 5),
            new Token(TokenType.COLON, ":", 1, 12),
            new Token(TokenType.INTEGER, "integer", 1, 14),
            new Token(TokenType.IS, "is", 1, 22),
            new Token(TokenType.INTEGER_LITERAL, "10", 1, 25),
            new Token(TokenType.SEMICOLON, ";", 1, 27),
            new Token(TokenType.WHILE, "while", 2, 1),
            new Token(TokenType.IDENTIFIER, "counter", 2, 7),
            new Token(TokenType.GREATER, ">", 2, 15),
            new Token(TokenType.INTEGER_LITERAL, "0", 2, 17),
            new Token(TokenType.LOOP, "loop", 2, 19),
            new Token(TokenType.PRINT, "print", 3, 5),
            new Token(TokenType.IDENTIFIER, "counter", 3, 11),
            new Token(TokenType.SEMICOLON, ";", 3, 18),
            new Token(TokenType.IDENTIFIER, "counter", 4, 5),
            new Token(TokenType.ASSIGN, ":=", 4, 13),
            new Token(TokenType.IDENTIFIER, "counter", 4, 16),
            new Token(TokenType.MINUS, "-", 4, 24),
            new Token(TokenType.INTEGER_LITERAL, "1", 4, 26),
            new Token(TokenType.SEMICOLON, ";", 4, 27),
            new Token(TokenType.END, "end", 5, 1),
            new Token(TokenType.EOF, "", 5, 4)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testForLoops() throws LexerException {
        String sourceCode = """
            for i in 1..10 loop
                print i * i;
            end
            for j in 10..1 reverse loop
                print j;
            end""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.FOR, "for", 1, 1),
            new Token(TokenType.IDENTIFIER, "i", 1, 5),
            new Token(TokenType.IN, "in", 1, 7),
            new Token(TokenType.INTEGER_LITERAL, "1", 1, 10),
            new Token(TokenType.RANGE, "..", 1, 11),
            new Token(TokenType.INTEGER_LITERAL, "10", 1, 13),
            new Token(TokenType.LOOP, "loop", 1, 16),
            new Token(TokenType.PRINT, "print", 2, 5),
            new Token(TokenType.IDENTIFIER, "i", 2, 11),
            new Token(TokenType.MULTIPLY, "*", 2, 13),
            new Token(TokenType.IDENTIFIER, "i", 2, 15),
            new Token(TokenType.SEMICOLON, ";", 2, 16),
            new Token(TokenType.END, "end", 3, 1),
            new Token(TokenType.FOR, "for", 4, 1),
            new Token(TokenType.IDENTIFIER, "j", 4, 5),
            new Token(TokenType.IN, "in", 4, 7),
            new Token(TokenType.INTEGER_LITERAL, "10", 4, 10),
            new Token(TokenType.RANGE, "..", 4, 12),
            new Token(TokenType.INTEGER_LITERAL, "1", 4, 14),
            new Token(TokenType.REVERSE, "reverse", 4, 16),
            new Token(TokenType.LOOP, "loop", 4, 24),
            new Token(TokenType.PRINT, "print", 5, 5),
            new Token(TokenType.IDENTIFIER, "j", 5, 11),
            new Token(TokenType.SEMICOLON, ";", 5, 12),
            new Token(TokenType.END, "end", 6, 1),
            new Token(TokenType.EOF, "", 6, 4)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testFunctionsRecursion() throws LexerException {
        String sourceCode = """
            routine factorial(n: integer): integer is
                if n <= 1 then
                    return 1;
                else
                    return n * factorial(n - 1);
                end
            end
            var result: integer is factorial(5);""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.ROUTINE, "routine", 1, 1),
            new Token(TokenType.IDENTIFIER, "factorial", 1, 9),
            new Token(TokenType.LPAREN, "(", 1, 18),
            new Token(TokenType.IDENTIFIER, "n", 1, 19),
            new Token(TokenType.COLON, ":", 1, 20),
            new Token(TokenType.INTEGER, "integer", 1, 22),
            new Token(TokenType.RPAREN, ")", 1, 29),
            new Token(TokenType.COLON, ":", 1, 30),
            new Token(TokenType.INTEGER, "integer", 1, 32),
            new Token(TokenType.IS, "is", 1, 40),
            new Token(TokenType.IF, "if", 2, 5),
            new Token(TokenType.IDENTIFIER, "n", 2, 8),
            new Token(TokenType.LESS_EQUAL, "<=", 2, 10),
            new Token(TokenType.INTEGER_LITERAL, "1", 2, 13),
            new Token(TokenType.THEN, "then", 2, 15),
            new Token(TokenType.RETURN, "return", 3, 9),
            new Token(TokenType.INTEGER_LITERAL, "1", 3, 16),
            new Token(TokenType.SEMICOLON, ";", 3, 17),
            new Token(TokenType.ELSE, "else", 4, 5),
            new Token(TokenType.RETURN, "return", 5, 9),
            new Token(TokenType.IDENTIFIER, "n", 5, 16),
            new Token(TokenType.MULTIPLY, "*", 5, 18),
            new Token(TokenType.IDENTIFIER, "factorial", 5, 20),
            new Token(TokenType.LPAREN, "(", 5, 29),
            new Token(TokenType.IDENTIFIER, "n", 5, 30),
            new Token(TokenType.MINUS, "-", 5, 32),
            new Token(TokenType.INTEGER_LITERAL, "1", 5, 34),
            new Token(TokenType.RPAREN, ")", 5, 35),
            new Token(TokenType.SEMICOLON, ";", 5, 36),
            new Token(TokenType.END, "end", 6, 5),
            new Token(TokenType.END, "end", 7, 1),
            new Token(TokenType.VAR, "var", 8, 1),
            new Token(TokenType.IDENTIFIER, "result", 8, 5),
            new Token(TokenType.COLON, ":", 8, 11),
            new Token(TokenType.INTEGER, "integer", 8, 13),
            new Token(TokenType.IS, "is", 8, 21),
            new Token(TokenType.IDENTIFIER, "factorial", 8, 24),
            new Token(TokenType.LPAREN, "(", 8, 33),
            new Token(TokenType.INTEGER_LITERAL, "5", 8, 34),
            new Token(TokenType.RPAREN, ")", 8, 35),
            new Token(TokenType.SEMICOLON, ";", 8, 36),
            new Token(TokenType.EOF, "", 8, 37)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testTypeConversions() throws LexerException {
        String sourceCode = """
            var i: integer is 42;
            var r: real is i;
            var b: boolean is 1;
            var converted: integer is true;""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "i", 1, 5),
            new Token(TokenType.COLON, ":", 1, 6),
            new Token(TokenType.INTEGER, "integer", 1, 8),
            new Token(TokenType.IS, "is", 1, 16),
            new Token(TokenType.INTEGER_LITERAL, "42", 1, 19),
            new Token(TokenType.SEMICOLON, ";", 1, 21),
            new Token(TokenType.VAR, "var", 2, 1),
            new Token(TokenType.IDENTIFIER, "r", 2, 5),
            new Token(TokenType.COLON, ":", 2, 6),
            new Token(TokenType.REAL, "real", 2, 8),
            new Token(TokenType.IS, "is", 2, 13),
            new Token(TokenType.IDENTIFIER, "i", 2, 16),
            new Token(TokenType.SEMICOLON, ";", 2, 17),
            new Token(TokenType.VAR, "var", 3, 1),
            new Token(TokenType.IDENTIFIER, "b", 3, 5),
            new Token(TokenType.COLON, ":", 3, 6),
            new Token(TokenType.BOOLEAN, "boolean", 3, 8),
            new Token(TokenType.IS, "is", 3, 16),
            new Token(TokenType.INTEGER_LITERAL, "1", 3, 19),
            new Token(TokenType.SEMICOLON, ";", 3, 20),
            new Token(TokenType.VAR, "var", 4, 1),
            new Token(TokenType.IDENTIFIER, "converted", 4, 5),
            new Token(TokenType.COLON, ":", 4, 14),
            new Token(TokenType.INTEGER, "integer", 4, 16),
            new Token(TokenType.IS, "is", 4, 24),
            new Token(TokenType.TRUE, "true", 4, 27),
            new Token(TokenType.SEMICOLON, ";", 4, 31),
            new Token(TokenType.EOF, "", 4, 32)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testErrorDetection() throws LexerException {
        String sourceCode = """
            var flag: boolean is 3.14;""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "flag", 1, 5),
            new Token(TokenType.COLON, ":", 1, 9),
            new Token(TokenType.BOOLEAN, "boolean", 1, 11),
            new Token(TokenType.IS, "is", 1, 19),
            new Token(TokenType.REAL_LITERAL, "3.14", 1, 22),
            new Token(TokenType.SEMICOLON, ";", 1, 26),
            new Token(TokenType.EOF, "", 1, 27)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testOperatorPrecedence() throws LexerException {
        String sourceCode = """
            var result: integer is 2 + 3 * 4 - 1;
            var comparison: boolean is (result > 10) and not (result = 15);""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "result", 1, 5),
            new Token(TokenType.COLON, ":", 1, 11),
            new Token(TokenType.INTEGER, "integer", 1, 13),
            new Token(TokenType.IS, "is", 1, 21),
            new Token(TokenType.INTEGER_LITERAL, "2", 1, 24),
            new Token(TokenType.PLUS, "+", 1, 26),
            new Token(TokenType.INTEGER_LITERAL, "3", 1, 28),
            new Token(TokenType.MULTIPLY, "*", 1, 30),
            new Token(TokenType.INTEGER_LITERAL, "4", 1, 32),
            new Token(TokenType.MINUS, "-", 1, 34),
            new Token(TokenType.INTEGER_LITERAL, "1", 1, 36),
            new Token(TokenType.SEMICOLON, ";", 1, 37),
            new Token(TokenType.VAR, "var", 2, 1),
            new Token(TokenType.IDENTIFIER, "comparison", 2, 5),
            new Token(TokenType.COLON, ":", 2, 15),
            new Token(TokenType.BOOLEAN, "boolean", 2, 17),
            new Token(TokenType.IS, "is", 2, 25),
            new Token(TokenType.LPAREN, "(", 2, 28),
            new Token(TokenType.IDENTIFIER, "result", 2, 29),
            new Token(TokenType.GREATER, ">", 2, 36),
            new Token(TokenType.INTEGER_LITERAL, "10", 2, 38),
            new Token(TokenType.RPAREN, ")", 2, 40),
            new Token(TokenType.AND, "and", 2, 42),
            new Token(TokenType.NOT, "not", 2, 46),
            new Token(TokenType.LPAREN, "(", 2, 50),
            new Token(TokenType.IDENTIFIER, "result", 2, 51),
            new Token(TokenType.EQUAL, "=", 2, 58),
            new Token(TokenType.INTEGER_LITERAL, "15", 2, 60),
            new Token(TokenType.RPAREN, ")", 2, 62),
            new Token(TokenType.SEMICOLON, ";", 2, 63),
            new Token(TokenType.EOF, "", 2, 64)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testComplexDataStructures() throws LexerException {
        String sourceCode =
            """
                type Student is record
                    var id: integer;
                    var grade: real;
                end
                var students: array[3] Student;
                students[1].id := 101;
                students[1].grade := 85.5;
                for student in students loop
                    print student.id, student.grade;
                end""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.TYPE, "type", 1, 1),
            new Token(TokenType.IDENTIFIER, "Student", 1, 6),
            new Token(TokenType.IS, "is", 1, 14),
            new Token(TokenType.RECORD, "record", 1, 17),
            new Token(TokenType.VAR, "var", 2, 5),
            new Token(TokenType.IDENTIFIER, "id", 2, 9),
            new Token(TokenType.COLON, ":", 2, 11),
            new Token(TokenType.INTEGER, "integer", 2, 13),
            new Token(TokenType.SEMICOLON, ";", 2, 20),
            new Token(TokenType.VAR, "var", 3, 5),
            new Token(TokenType.IDENTIFIER, "grade", 3, 9),
            new Token(TokenType.COLON, ":", 3, 14),
            new Token(TokenType.REAL, "real", 3, 16),
            new Token(TokenType.SEMICOLON, ";", 3, 20),
            new Token(TokenType.END, "end", 4, 1),
            new Token(TokenType.VAR, "var", 5, 1),
            new Token(TokenType.IDENTIFIER, "students", 5, 5),
            new Token(TokenType.COLON, ":", 5, 13),
            new Token(TokenType.ARRAY, "array", 5, 15),
            new Token(TokenType.LBRACKET, "[", 5, 20),
            new Token(TokenType.INTEGER_LITERAL, "3", 5, 21),
            new Token(TokenType.RBRACKET, "]", 5, 22),
            new Token(TokenType.IDENTIFIER, "Student", 5, 24),
            new Token(TokenType.SEMICOLON, ";", 5, 31),
            new Token(TokenType.IDENTIFIER, "students", 6, 1),
            new Token(TokenType.LBRACKET, "[", 6, 9),
            new Token(TokenType.INTEGER_LITERAL, "1", 6, 10),
            new Token(TokenType.RBRACKET, "]", 6, 11),
            new Token(TokenType.DOT, ".", 6, 12),
            new Token(TokenType.IDENTIFIER, "id", 6, 13),
            new Token(TokenType.ASSIGN, ":=", 6, 16),
            new Token(TokenType.INTEGER_LITERAL, "101", 6, 19),
            new Token(TokenType.SEMICOLON, ";", 6, 22),
            new Token(TokenType.IDENTIFIER, "students", 7, 1),
            new Token(TokenType.LBRACKET, "[", 7, 9),
            new Token(TokenType.INTEGER_LITERAL, "1", 7, 10),
            new Token(TokenType.RBRACKET, "]", 7, 11),
            new Token(TokenType.DOT, ".", 7, 12),
            new Token(TokenType.IDENTIFIER, "grade", 7, 13),
            new Token(TokenType.ASSIGN, ":=", 7, 19),
            new Token(TokenType.REAL_LITERAL, "85.5", 7, 22),
            new Token(TokenType.SEMICOLON, ";", 7, 26),
            new Token(TokenType.FOR, "for", 8, 1),
            new Token(TokenType.IDENTIFIER, "student", 8, 5),
            new Token(TokenType.IN, "in", 8, 13),
            new Token(TokenType.IDENTIFIER, "students", 8, 16),
            new Token(TokenType.LOOP, "loop", 8, 25),
            new Token(TokenType.PRINT, "print", 9, 5),
            new Token(TokenType.IDENTIFIER, "student", 9, 11),
            new Token(TokenType.DOT, ".", 9, 18),
            new Token(TokenType.IDENTIFIER, "id", 9, 19),
            new Token(TokenType.COMMA, ",", 9, 21),
            new Token(TokenType.IDENTIFIER, "student", 9, 23),
            new Token(TokenType.DOT, ".", 9, 30),
            new Token(TokenType.IDENTIFIER, "grade", 9, 31),
            new Token(TokenType.SEMICOLON, ";", 9, 36),
            new Token(TokenType.END, "end", 10, 1),
            new Token(TokenType.EOF, "", 10, 4)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    @Test
    public void testNestedRecords() throws LexerException {
        String sourceCode =
            """
                type Address is record
                    var street: string;
                    var city: string;
                    var zip: integer;
                end
                type Person is record
                    var name: string;
                    var age: integer;
                    var address: Address;
                end
                var person: Person;
                person.name := "John Doe";
                person.age := 30;
                person.address.street := "123 Main St";
                person.address.city := "New York";
                person.address.zip := 10001;""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.TYPE, "type", 1, 1),
            new Token(TokenType.IDENTIFIER, "Address", 1, 6),
            new Token(TokenType.IS, "is", 1, 14),
            new Token(TokenType.RECORD, "record", 1, 17),
            new Token(TokenType.VAR, "var", 2, 5),
            new Token(TokenType.IDENTIFIER, "street", 2, 9),
            new Token(TokenType.COLON, ":", 2, 15),
            new Token(TokenType.IDENTIFIER, "string", 2, 17),
            new Token(TokenType.SEMICOLON, ";", 2, 23),
            new Token(TokenType.VAR, "var", 3, 5),
            new Token(TokenType.IDENTIFIER, "city", 3, 9),
            new Token(TokenType.COLON, ":", 3, 13),
            new Token(TokenType.IDENTIFIER, "string", 3, 15),
            new Token(TokenType.SEMICOLON, ";", 3, 21),
            new Token(TokenType.VAR, "var", 4, 5),
            new Token(TokenType.IDENTIFIER, "zip", 4, 9),
            new Token(TokenType.COLON, ":", 4, 12),
            new Token(TokenType.INTEGER, "integer", 4, 14),
            new Token(TokenType.SEMICOLON, ";", 4, 21),
            new Token(TokenType.END, "end", 5, 1),
            new Token(TokenType.TYPE, "type", 6, 1),
            new Token(TokenType.IDENTIFIER, "Person", 6, 6),
            new Token(TokenType.IS, "is", 6, 13),
            new Token(TokenType.RECORD, "record", 6, 16),
            new Token(TokenType.VAR, "var", 7, 5),
            new Token(TokenType.IDENTIFIER, "name", 7, 9),
            new Token(TokenType.COLON, ":", 7, 13),
            new Token(TokenType.IDENTIFIER, "string", 7, 15),
            new Token(TokenType.SEMICOLON, ";", 7, 21),
            new Token(TokenType.VAR, "var", 8, 5),
            new Token(TokenType.IDENTIFIER, "age", 8, 9),
            new Token(TokenType.COLON, ":", 8, 12),
            new Token(TokenType.INTEGER, "integer", 8, 14),
            new Token(TokenType.SEMICOLON, ";", 8, 21),
            new Token(TokenType.VAR, "var", 9, 5),
            new Token(TokenType.IDENTIFIER, "address", 9, 9),
            new Token(TokenType.COLON, ":", 9, 16),
            new Token(TokenType.IDENTIFIER, "Address", 9, 18),
            new Token(TokenType.SEMICOLON, ";", 9, 25),
            new Token(TokenType.END, "end", 10, 1),
            new Token(TokenType.VAR, "var", 11, 1),
            new Token(TokenType.IDENTIFIER, "person", 11, 5),
            new Token(TokenType.COLON, ":", 11, 11),
            new Token(TokenType.IDENTIFIER, "Person", 11, 13),
            new Token(TokenType.SEMICOLON, ";", 11, 19),
            new Token(TokenType.IDENTIFIER, "person", 12, 1),
            new Token(TokenType.DOT, ".", 12, 7),
            new Token(TokenType.IDENTIFIER, "name", 12, 8),
            new Token(TokenType.ASSIGN, ":=", 12, 13),
            new Token(TokenType.STRING_LITERAL, "\"John Doe\"", 12, 16),
            new Token(TokenType.SEMICOLON, ";", 12, 26),
            new Token(TokenType.IDENTIFIER, "person", 13, 1),
            new Token(TokenType.DOT, ".", 13, 7),
            new Token(TokenType.IDENTIFIER, "age", 13, 8),
            new Token(TokenType.ASSIGN, ":=", 13, 12),
            new Token(TokenType.INTEGER_LITERAL, "30", 13, 15),
            new Token(TokenType.SEMICOLON, ";", 13, 17),
            new Token(TokenType.IDENTIFIER, "person", 14, 1),
            new Token(TokenType.DOT, ".", 14, 7),
            new Token(TokenType.IDENTIFIER, "address", 14, 8),
            new Token(TokenType.DOT, ".", 14, 15),
            new Token(TokenType.IDENTIFIER, "street", 14, 16),
            new Token(TokenType.ASSIGN, ":=", 14, 23),
            new Token(TokenType.STRING_LITERAL, "\"123 Main St\"", 14, 26),
            new Token(TokenType.SEMICOLON, ";", 14, 39),
            new Token(TokenType.IDENTIFIER, "person", 15, 1),
            new Token(TokenType.DOT, ".", 15, 7),
            new Token(TokenType.IDENTIFIER, "address", 15, 8),
            new Token(TokenType.DOT, ".", 15, 15),
            new Token(TokenType.IDENTIFIER, "city", 15, 16),
            new Token(TokenType.ASSIGN, ":=", 15, 21),
            new Token(TokenType.STRING_LITERAL, "\"New York\"", 15, 24),
            new Token(TokenType.SEMICOLON, ";", 15, 34),
            new Token(TokenType.IDENTIFIER, "person", 16, 1),
            new Token(TokenType.DOT, ".", 16, 7),
            new Token(TokenType.IDENTIFIER, "address", 16, 8),
            new Token(TokenType.DOT, ".", 16, 15),
            new Token(TokenType.IDENTIFIER, "zip", 16, 16),
            new Token(TokenType.ASSIGN, ":=", 16, 20),
            new Token(TokenType.INTEGER_LITERAL, "10001", 16, 23),
            new Token(TokenType.SEMICOLON, ";", 16, 28),
            new Token(TokenType.EOF, "", 16, 29)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }
}
