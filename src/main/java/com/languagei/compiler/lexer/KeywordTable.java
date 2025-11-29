package com.languagei.compiler.lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyword lookup table for fast identification
 */
public class KeywordTable {
    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        // Declaration keywords
        keywords.put("var", TokenType.VAR);
        keywords.put("type", TokenType.TYPE);
        keywords.put("routine", TokenType.ROUTINE);
        keywords.put("is", TokenType.IS);
        keywords.put("end", TokenType.END);
        keywords.put("record", TokenType.RECORD);
        keywords.put("array", TokenType.ARRAY);

        // Type keywords
        keywords.put("integer", TokenType.INTEGER_TYPE);
        keywords.put("real", TokenType.REAL_TYPE);
        keywords.put("boolean", TokenType.BOOLEAN_TYPE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);

        // Control flow keywords
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("loop", TokenType.LOOP);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("reverse", TokenType.REVERSE);
        keywords.put("return", TokenType.RETURN);

        // Special keywords
        keywords.put("print", TokenType.PRINT);

        // Logical operators
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("xor", TokenType.XOR);
        keywords.put("not", TokenType.NOT);
    }

    public static TokenType lookup(String word) {
        return keywords.getOrDefault(word, null);
    }

    public static boolean isKeyword(String word) {
        return keywords.containsKey(word);
    }
}

