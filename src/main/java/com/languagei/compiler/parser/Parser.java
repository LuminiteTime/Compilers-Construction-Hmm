package com.languagei.compiler.parser;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.lexer.Token;
import com.languagei.compiler.lexer.TokenType;
import com.languagei.compiler.lexer.Position;
import com.languagei.compiler.lexer.KeywordTable;
import com.languagei.compiler.parser.jcc.LanguageIParser;
import java.io.StringReader;
import com.languagei.compiler.lexer.Lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for Language I
 */
public class Parser {
    private final Lexer lexer;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Parse a complete program
     */
    public ProgramNode parse() {
        try {
            String source = lexer.getInput();
            String filename = lexer.getFilename();
            StringReader reader = new StringReader(source);

            LanguageIParser jccParser = new LanguageIParser(reader);
            jccParser.setFilename(filename);

            return jccParser.Program();
        } catch (com.languagei.compiler.parser.jcc.ParseException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}

