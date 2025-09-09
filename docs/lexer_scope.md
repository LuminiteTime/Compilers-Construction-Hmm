You are an expert compiler engineer specializing in lexical analysis, with deep knowledge from compiler construction principles including finite state machines, regular expressions, token representations, and scanner-parser integration as described in standard lectures.

Your assignment: **implement a complete, error-free lexer for the “Imperative (I)” language** fully based on the provided specification extracted from the language definition and related materials. Produce a single Java file that integrates with a Bison-generated parser, targeting WebAssembly (WASM) as the eventual platform, though the lexer itself is platform-agnostic.

====================================================================
LANGUAGE “I” — DETAILED TOKEN SPECIFICATION
--------------------------------------------------------------------
The language processes source text as a sequence of Unicode characters from a single disk file. Syntactically, a program is a sequence of declarations (variables, types, subprograms) separated by newlines or semicolons. All entities must be declared before use, with scopes spanning from declaration to the end of the block or program. Simple declarations (variables, types) can be in any scope; routine declarations are top-level only (no nested routines).

Extracted Token Categories (from grammar rules):
1. Keywords (reserved words with special meaning; distinguish from identifiers via lookup):
   - Declarations: var, type, is, integer, real, boolean, array, record, end
   - Statements: while, loop, for, in, reverse, if, then, else, print, routine
   - Expressions/Booleans: true, false, and, or, xor, not
   - Other: return (implied in routine bodies for functions)

2. Identifiers  
   - Used for variable names, type names, routine names, parameters.  
   - Regex (inferred from typical patterns in formal basics): letter [letter | digit]* where letter is a-zA-Z or underscore (_), starting with letter or underscore.  
   - Distinguish from keywords using a HashMap lookup (O(1) efficiency). If match, return keyword token; else identifier.  
   - Case-sensitive (assumed, as examples use lowercase).

3. Literals  
   - IntegerLiteral: sequence of decimal digits (0-9), optionally prefixed by Sign (+ or -). Represents positive/negative integer values.  
   - RealLiteral: sequence with decimal point (e.g., digits.digits), optionally prefixed by Sign. Represents positive/negative real values.  
   - Boolean: true or false (treated as keywords but represent boolean literals).  
   - Note: String literals are not explicitly defined in the grammar, but test examples include them (e.g., "test"); handle as "([^"\n\\]|\\.)*" for completeness, returning STRING_LITERAL token.

4. Operators (from expression grammar; use maximal munch for composites):  
   - Arithmetic: + - * / % (in Simple and Factor)  
   - Relational: < <= > >= = /= (in Relation)  
   - Logical: and or xor (in Expression), not (in Primary)  
   - Assignment: := (in Assignment)  
   - Range: .. (in ForLoop Range)  
   - Sign: + - (in Primary for literals)

5. Delimiters / Punctuation:  
   - : (in declarations, parameters)  
   - ; (statement/declaration separator; optional if newline used)  
   - , (in argument lists, print)  
   - . (in dotted notation for records/arrays)  
   - ( ) (in routine calls, parameters, expressions)  
   - [ ] (in array types and indexing)  

6. Whitespace:  
   - Spaces, tabs, newlines (\n), horizontal tabs (\t).  
   - Treated as non-meaningful separators; skip them. Newlines separate declarations/statements like semicolons.  
   - Do not include in tokens except where part of literals (none in this language).

7. Comments:  
   - Not explicitly in language spec, but from lexical theory: support short (// to newline) and long (/* ... */ non-nesting) comments, treating as whitespace (skip, no token).  
   - Drop them as they do not alter semantics.

8. End-of-File (EOF): Special token at input end.  

9. Errors:  
   - Invalid characters or malformed tokens (e.g., unterminated string if supported, invalid number like isolated sign) throw LexerException with message, line, and column.  
   - Report lexical errors precisely using source coordinates (span: line number, start/end position).

Additional Rules from Spec:  
- Tokens are minimal language units (e.g., operator signs, delimiters, identifiers, keywords, literals).  
- Use regular grammars/expressions for lexeme structure (e.g., identifier -> letter [letter | digit]*).  
- No preprocessing (no #include or macros).  
- Reference types for user-defined (arrays, records), but lexer doesn't handle semantics—just tokens.

====================================================================
LEXICAL ANALYSIS THEORY & IMPLEMENTATION GUIDELINES
--------------------------------------------------------------------
- **Purpose**: Convert source character sequence into token sequence, identifying minimal meaningful units.  
- **Finite State Machine (FSM)**: Implement scanner as FSM with states (e.g., initial, identifier, number, operator). Transition on input char, using one lookahead char if needed.  
- **Maximal Munch**: Always take the longest possible token (e.g., <= over <, .. over .).  
- **Token Representation**: Simple (integer code) or structured (class with code, attributes like value for literals/identifiers, span for position). Use OO: base Token class with subclasses if advanced, or single class with fields.  
- **Keyword vs Identifier**: After scanning identifier-like string, check against fixed keyword set (e.g., via hash function or switch for efficiency).  
- **Number Handling**: Scan digits, handle optional sign and decimal for real vs integer. Convert to binary internally if needed, but store lexeme string.  
- **Integration with Parser**: Provide getNextToken() returning Token on demand (classic case for simple grammars). Compatible with Bison (yacc-like) parser generator.  
- **Non-Standard**: No ambiguities like context-dependent tokens (e.g., PL/I IF); no user-defined literals; no lookahead beyond one char.  
- **Efficiency**: Hash for keywords; buffer input for scanning.

Technology Stack (from project details):  
- Implementation: Java  
- Parser Tool: Bison-based  
- Target: WebAssembly (WASM)  

====================================================================
REQUIRED JAVA API (can be modified if needed)
--------------------------------------------------------------------
enum TokenType { /* one for each terminal: e.g., VAR, IDENTIFIER, INTEGER_LITERAL, ASSIGN, etc. Include EOF */ }  

class Token {  
    TokenType type;  
    String lexeme;  // raw string  
    int line;       // starting line  
    int column;     // starting column  
    // Optional: endLine, endColumn for full span  
}  

class Lexer {  
    Lexer(Reader source);  // e.g., BufferedReader  
    Token nextToken() throws LexerException;  // returns EOF token at end  
}  

class LexerException extends Exception { /* with message, line, col */ }  

Constraints:  
- Pure Java 8+ standard library (e.g., java.io.BufferedReader, no external deps).  
- O(1) keyword recognition (HashMap<String, TokenType>).  
- Finite-state scanning; manual char-by-char, no java.util.regex.  
- Accurate tracking: line starts at 1, column at 1; increment on \n.  
- Robust: Handle EOF gracefully; never crash on bad input.  
- Test Integration: Include main() to run all tests below, printing token sequences or errors.

====================================================================
TEST SUITE (exact code examples from slides; lexer must tokenize without lexical errors unless invalid)
--------------------------------------------------------------------
Test 1: Variable Declarations  
var x: integer is 42;  
var y: real is 3.14;  
var flag: boolean is true;  
var name is "test";  
Expected: Successful tokenization with explicit types and inference (handle string as literal).

Test 2: Arrays & Data Structures  
var numbers: array[5] integer;  
numbers[1] := 10;  
numbers[2] := 20;  
var sum: integer is numbers[1] + numbers[2];  
Expected: Array decl, assignment, 1-based indexing tokens.

Test 3: Record Types  
type Point is record  
    var x: real;  
    var y: real;  
end  
var p1: Point;  
p1.x := 1.5;  
p1.y := 2.7;  
Expected: Record type def, dot access.

Test 4: While Loops  
var counter: integer is 10;  
while counter > 0 loop  
    print counter;  
    counter := counter - 1;  
end  
Expected: Loop tokens.

Test 5: For Loops  
for i in 1..10 loop  
    print i * i;  
end  
for j in 10..1 reverse loop  
    print j;  
end  
Expected: Range and reverse.

Test 6: Functions & Recursion  
routine factorial(n: integer): integer is  
    if n <= 1 then  
        return 1;  
    else  
        return n * factorial(n - 1);  
    end  
end  
var result: integer is factorial(5);  
Expected: Routine, if, return, call.

Test 7: Type Conversions  
var i: integer is 42;  
var r: real is i;  
var b: boolean is 1;  
var converted: integer is true;  
Expected: Declarations and literals.

Test 8: Error Detection  
var flag: boolean is 3.14;  
Expected: Valid lexically (error is semantic).

Test 9: Operator Precedence  
var result: integer is 2 + 3 * 4 - 1;  
var comparison: boolean is (result > 10) and not (result = 15);  
Expected: Operator tokens (precedence for parser).

Test 10: Complex Data Structures  
type Student is record  
    var id: integer;  
    var grade: real;  
end  
var students: array[3] Student;  
students[1].id := 101;  
students[1].grade := 85.5;  
for student in students loop  
    print student.id, student.grade;  
end  
Expected: Nested types, loop over array.

====================================================================
DELIVERABLES
--------------------------------------------------------------------
1. Full, compilable Java code implementing the lexer.  
2. Brief README section in code comments: For each test, list first 10 tokens (type:lexeme@line:col) and confirm no LexerException (except if invalid lexeme).  
3. Integration Note: Generate %token <type> IDENTIFIER etc. in Bison .y file matching TokenType enums.

Focus on clean, modular code with inline comments for FSM logic. Return only the code and README—no extra text.
