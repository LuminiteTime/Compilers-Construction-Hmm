package com.languagei.compiler.lexer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Lexical analyzer for Language I
 */
public class Lexer {
    private final String input;
    private final String filename;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    private int tokenStartLine;
    private int tokenStartColumn;

    public Lexer(String input, String filename) {
        this.input = input;
        this.filename = filename;
    }

    public static Lexer fromFile(String filename) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
        return new Lexer(content, filename);
    }

    /**
     * Get the next token from input
     */
    public Token nextToken() {
        skipWhitespaceAndComments();

        if (isAtEnd()) {
            return makeToken(TokenType.EOF, "");
        }

        tokenStartLine = line;
        tokenStartColumn = column;
        char c = peek();

        // Single character tokens
        switch (c) {
            case '(': advance(); return makeToken(TokenType.LPAREN, "(");
            case ')': advance(); return makeToken(TokenType.RPAREN, ")");
            case '[': advance(); return makeToken(TokenType.LBRACKET, "[");
            case ']': advance(); return makeToken(TokenType.RBRACKET, "]");
            case ',': advance(); return makeToken(TokenType.COMMA, ",");
            case ';': advance(); return makeToken(TokenType.SEMICOLON, ";");
            case '+': advance(); return makeToken(TokenType.PLUS, "+");
            case '*': advance(); return makeToken(TokenType.STAR, "*");
            case '%': advance(); return makeToken(TokenType.PERCENT, "%");
        }

        // Multi-character operators
        if (c == '=') {
            advance();
            if (peek() == '>') {
                advance();
                return makeToken(TokenType.ARROW, "=>");
            }
            return makeToken(TokenType.EQ, "=");
        }

        if (c == '-') {
            advance();
            if (peek() == '>') {
                advance();
                return makeToken(TokenType.ARROW, "->");
            }
            return makeToken(TokenType.MINUS, "-");
        }

        if (c == ':') {
            advance();
            if (peek() == '=') {
                advance();
                return makeToken(TokenType.ASSIGN, ":=");
            }
            return makeToken(TokenType.COLON, ":");
        }

        if (c == '.') {
            advance();
            if (peek() == '.') {
                advance();
                return makeToken(TokenType.RANGE, "..");
            }
            return makeToken(TokenType.DOT, ".");
        }

        if (c == '<') {
            advance();
            if (peek() == '=') {
                advance();
                return makeToken(TokenType.LE, "<=");
            }
            return makeToken(TokenType.LT, "<");
        }

        if (c == '>') {
            advance();
            if (peek() == '=') {
                advance();
                return makeToken(TokenType.GE, ">=");
            }
            return makeToken(TokenType.GT, ">");
        }

        if (c == '=') {
            advance();
            return makeToken(TokenType.EQ, "=");
        }

        if (c == '/') {
            advance();
            if (!isAtEnd() && peek() == '=') {
                advance();
                return makeToken(TokenType.NE, "/=");
            }
            return makeToken(TokenType.SLASH, "/");
        }

        // Numbers
        if (Character.isDigit(c)) {
            return scanNumber();
        }

        // Identifiers and keywords
        if (Character.isLetter(c) || c == '_') {
            return scanIdentifier();
        }

        throw new LexerException("Unexpected character '" + c + "' at " + getCurrentPosition());
    }

    private Token scanNumber() {
        int start = position;
        while (!isAtEnd() && Character.isDigit(peek())) {
            advance();
        }

        // Check for decimal point
        if (!isAtEnd() && peek() == '.' && position + 1 < input.length() && Character.isDigit(input.charAt(position + 1))) {
            advance(); // consume '.'
            while (!isAtEnd() && Character.isDigit(peek())) {
                advance();
            }

            // Check for scientific notation
            if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
                advance();
                if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                    advance();
                }
                while (!isAtEnd() && Character.isDigit(peek())) {
                    advance();
                }
            }

            String lexeme = input.substring(start, position);
            return makeToken(TokenType.REAL_LITERAL, lexeme, Double.parseDouble(lexeme));
        }

        String lexeme = input.substring(start, position);
        return makeToken(TokenType.INTEGER_LITERAL, lexeme, Long.parseLong(lexeme));
    }

    private Token scanIdentifier() {
        int start = position;
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            advance();
        }

        String lexeme = input.substring(start, position);
        TokenType type = KeywordTable.lookup(lexeme);
        if (type != null) {
            if (type == TokenType.TRUE) {
                return makeToken(type, lexeme, true);
            } else if (type == TokenType.FALSE) {
                return makeToken(type, lexeme, false);
            }
            return makeToken(type, lexeme);
        }
        return makeToken(TokenType.IDENTIFIER, lexeme);
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();

            if (c == ' ' || c == '\t' || c == '\r') {
                advance();
            } else if (c == '\n') {
                advance();
            } else if (c == '/' && position + 1 < input.length()) {
                if (input.charAt(position + 1) == '/') {
                    // Single-line comment
                    while (!isAtEnd() && peek() != '\n') {
                        advance();
                    }
                } else if (input.charAt(position + 1) == '*') {
                    // Multi-line comment
                    advance(); // consume '/'
                    advance(); // consume '*'
                    while (!isAtEnd()) {
                        if (peek() == '*' && position + 1 < input.length() && input.charAt(position + 1) == '/') {
                            advance(); // consume '*'
                            advance(); // consume '/'
                            break;
                        }
                        advance();
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private Token makeToken(TokenType type, String lexeme) {
        return makeToken(type, lexeme, null);
    }

    private Token makeToken(TokenType type, String lexeme, Object literal) {
        Position pos = new Position(tokenStartLine, tokenStartColumn, position - lexeme.length(), filename);
        return new Token(type, lexeme, literal, pos);
    }

    private boolean isAtEnd() {
        return position >= input.length();
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return input.charAt(position);
    }

    private void advance() {
        if (!isAtEnd()) {
            if (input.charAt(position) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }

    private Position getCurrentPosition() {
        return new Position(line, column, position, filename);
    }

    public void reset() {
        position = 0;
        line = 1;
        column = 1;
    }

    public Position getPosition() {
        return getCurrentPosition();
    }

    public String getInput() {
        return input;
    }

    public String getFilename() {
        return filename;
    }
}
