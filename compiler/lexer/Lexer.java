import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Lexer {
    private final Reader reader;
    private int currentChar;
    private int line = 1;
    private int column = 1;
    private boolean eofReached = false;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("integer", TokenType.INTEGER);
        KEYWORDS.put("real", TokenType.REAL);
        KEYWORDS.put("boolean", TokenType.BOOLEAN);
        KEYWORDS.put("array", TokenType.ARRAY);
        KEYWORDS.put("record", TokenType.RECORD);
        KEYWORDS.put("end", TokenType.END);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("loop", TokenType.LOOP);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("reverse", TokenType.REVERSE);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("routine", TokenType.ROUTINE);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("xor", TokenType.XOR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("return", TokenType.RETURN);
    }

    public Lexer(Reader reader) throws LexerException {
        this.reader = reader;
        try {
            this.currentChar = reader.read();
        } catch (IOException e) {
            throw new LexerException("Failed to read from input source", line, column, e);
        }
    }

    public Token nextToken() throws LexerException {
        while (!eofReached) {
            int startLine = line;
            int startColumn = column;

            switch (currentChar) {
                case -1 -> {
                    eofReached = true;
                    return new Token(TokenType.EOF, "", line, column);
                }

                case ' ', '\t', '\r' -> {
                    advance();
                }

                case '\n' -> {
                    advance();
                }
                case ';' -> {
                    advance();
                    return new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
                }
                case ',' -> {
                    advance();
                    return new Token(TokenType.COMMA, ",", startLine, startColumn);
                }
                case '(' -> {
                    advance();
                    return new Token(TokenType.LPAREN, "(", startLine, startColumn);
                }
                case ')' -> {
                    advance();
                    return new Token(TokenType.RPAREN, ")", startLine, startColumn);
                }
                case '[' -> {
                    advance();
                    return new Token(TokenType.LBRACKET, "[", startLine, startColumn);
                }
                case ']' -> {
                    advance();
                    return new Token(TokenType.RBRACKET, "]", startLine, startColumn);
                }
                case '+' -> {
                    advance();
                    return new Token(TokenType.PLUS, "+", startLine, startColumn);
                }
                case '*' -> {
                    advance();
                    return new Token(TokenType.MULTIPLY, "*", startLine, startColumn);
                }
                case '%' -> {
                    advance();
                    return new Token(TokenType.MODULO, "%", startLine, startColumn);
                }

                case '-' -> {
                    return scanMinusOrComment(startLine, startColumn);
                }
                case '/' -> {
                    return scanDivideOrComment(startLine, startColumn);
                }
                case '<' -> {
                    return scanLessOrLessEqual(startLine, startColumn);
                }
                case '>' -> {
                    return scanGreaterOrGreaterEqual(startLine, startColumn);
                }
                case '=' -> {
                    return scanEqualOrNotEqual(startLine, startColumn);
                }
                case ':' -> {
                    return scanColonOrAssign(startLine, startColumn);
                }
                case '.' -> {
                    return scanDotOrRange(startLine, startColumn);
                }
                case '"' -> {
                    return scanStringLiteral(startLine, startColumn);
                }

                default -> {
                    if (Character.isLetter(currentChar) || currentChar == '_') {
                        return scanIdentifierOrKeyword(startLine, startColumn);
                    } else if (Character.isDigit(currentChar) || currentChar == '+' || currentChar == '-') {
                        return scanNumberLiteral(startLine, startColumn);
                    } else {
                        throw new LexerException(
                            String.format("Invalid character: '%c' (ASCII %d)", (char)currentChar, currentChar),
                            line, column);
                    }
                }
            }
        }

        return new Token(TokenType.EOF, "", line, column);
    }

    private Token scanMinusOrComment(int startLine, int startColumn) throws LexerException {
        advance();
        return new Token(TokenType.MINUS, "-", startLine, startColumn);
    }

    private Token scanDivideOrComment(int startLine, int startColumn) throws LexerException {
        advance();

        switch (currentChar) {
            case '/':
                advance();
                while (currentChar != '\n' && currentChar != -1) {
                    advance();
                }
                return nextToken();

            case '*':
                advance();
                while (true) {
                    switch (currentChar) {
                        case -1 -> throw new LexerException("Unterminated multi-line comment", startLine, startColumn);
                        case '*' -> {
                            advance();
                            if (currentChar == '/') {
                                advance();
                                return nextToken();
                            }
                        }
                        default -> advance();
                    }
                }

            case '=':
                advance();
                return new Token(TokenType.NOT_EQUAL, "/=", startLine, startColumn, line, column);

            default:
                return new Token(TokenType.DIVIDE, "/", startLine, startColumn);
        }
    }

    private Token scanLessOrLessEqual(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.LESS_EQUAL, "<=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.LESS, "<", startLine, startColumn);
        }
    }

    private Token scanGreaterOrGreaterEqual(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.GREATER, ">", startLine, startColumn);
        }
    }

    private Token scanEqualOrNotEqual(int startLine, int startColumn) throws LexerException {
        advance();
        return new Token(TokenType.EQUAL, "=", startLine, startColumn);
    }

    private Token scanColonOrAssign(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '=') {
            advance();
            return new Token(TokenType.ASSIGN, ":=", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.COLON, ":", startLine, startColumn);
        }
    }

    private Token scanDotOrRange(int startLine, int startColumn) throws LexerException {
        advance();
        if (currentChar == '.') {
            advance();
            return new Token(TokenType.RANGE, "..", startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.DOT, ".", startLine, startColumn);
        }
    }

    private Token scanStringLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        advance();

        while (currentChar != '"' && currentChar != -1) {
            switch (currentChar) {
                case '\\' -> {
                    lexeme.append((char) currentChar);
                    advance();
                    if (currentChar != -1) {
                        lexeme.append((char) currentChar);
                        advance();
                    }
                }

                case '\n' -> throw new LexerException("Unterminated string literal", startLine, startColumn);

                default -> {
                    lexeme.append((char) currentChar);
                    advance();
                }
            }
        }

        if (currentChar == -1) {
            throw new LexerException("Unterminated string literal", startLine, startColumn);
        }

        lexeme.append('"');
        advance();

        return new Token(TokenType.STRING_LITERAL, lexeme.toString(), startLine, startColumn, line, column);
    }

    private Token scanIdentifierOrKeyword(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();

        if (!Character.isLetter(currentChar) && currentChar != '_') {
            throw new LexerException("Invalid identifier start character", line, column);
        }

        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            lexeme.append((char) currentChar);
            advance();
        }

        String identifier = lexeme.toString();

        TokenType type = KEYWORDS.get(identifier);
        if (type != null) {
            return new Token(type, identifier, startLine, startColumn, line, column);
        } else {
            return new Token(TokenType.IDENTIFIER, identifier, startLine, startColumn, line, column);
        }
    }

    private Token scanNumberLiteral(int startLine, int startColumn) throws LexerException {
        StringBuilder lexeme = new StringBuilder();
        boolean hasDecimalPoint = false;
        boolean hasDigits = false;

        if (currentChar == '+' || currentChar == '-') {
            lexeme.append((char) currentChar);
            advance();
        }

        while (Character.isDigit(currentChar)) {
            lexeme.append((char) currentChar);
            hasDigits = true;
            advance();
        }

        if (currentChar == '.') {
            hasDecimalPoint = true;
            lexeme.append((char) currentChar);
            advance();

            while (Character.isDigit(currentChar)) {
                lexeme.append((char) currentChar);
                hasDigits = true;
                advance();
            }
        }

        if (!hasDigits) {
            throw new LexerException("Invalid number format: no digits found", startLine, startColumn);
        }

        String numberStr = lexeme.toString();
        TokenType type = hasDecimalPoint ? TokenType.REAL_LITERAL : TokenType.INTEGER_LITERAL;

        return new Token(type, numberStr, startLine, startColumn, line, column);
    }

    private void advance() throws LexerException {
        try {
            if (currentChar == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            currentChar = reader.read();
        } catch (IOException e) {
            throw new LexerException("Failed to read next character", line, column, e);
        }
    }
}
