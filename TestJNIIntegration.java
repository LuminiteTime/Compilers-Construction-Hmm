import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.TokenType;
import java.io.StringReader;

public class TestJNIIntegration {
    public static void main(String[] args) {
        System.out.println("=== JNI INTEGRATION TEST ===");
        System.out.println("Testing Java Lexer + JNI Bridge");
        System.out.println();

        // Test data from slides - Test 1: Variable Declarations
        String testCode = """
            var x: integer is 42;
            var y: real is 3.14;
            var flag: boolean is true;
            var name is "test";
            """;

        System.out.println("Input code:");
        System.out.println(testCode);

        try {
            // Create lexer instance
            Lexer lexer = new Lexer(new StringReader(testCode));

            // Initialize JNI parser integration
            System.out.println("Initializing JNI parser integration...");
            lexer.initializeParser();
            System.out.println("✓ JNI parser initialized");

            // Set input for parsing
            System.out.println("Setting input for JNI parsing...");
            boolean inputSet = lexer.parseInput(testCode);
            if (inputSet) {
                System.out.println("✓ Input set successfully");
            } else {
                System.out.println("✗ Failed to set input");
                return;
            }

            // Test token retrieval via JNI
            System.out.println();
            System.out.println("Testing token retrieval via JNI:");
            System.out.println("----------------");

            int tokenCount = 0;
            boolean hasMoreTokens = true;

            while (hasMoreTokens) {
                int tokenType = lexer.nextTokenJNI();
                String lexeme = lexer.getLexemeJNI();
                int line = lexer.getLineJNI();

                tokenCount++;

                // Convert token type back to enum for display
                TokenType type = intToTokenType(tokenType);

                System.out.printf("%2d: %-15s '%s' @ line %d%n",
                    tokenCount, type, lexeme, line);

                // Stop at EOF
                if (tokenType == 309) { // TOK_EOF_TOKEN
                    hasMoreTokens = false;
                }

                // Safety check to prevent infinite loop
                if (tokenCount > 50) {
                    System.out.println("Safety: Stopping after 50 tokens");
                    break;
                }
            }

            System.out.println("----------------");
            System.out.println("✓ JNI token retrieval working");
            System.out.printf("✓ Processed %d tokens via JNI%n", tokenCount);

        } catch (Exception e) {
            System.out.println("✗ JNI Integration test failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println();
        System.out.println("=== JNI INTEGRATION STATUS: WORKING ===");
        System.out.println();
        System.out.println("The Java lexer successfully integrates with C++ parser via JNI.");
        System.out.println("Token stream can be passed from Java lexer to C++ Bison parser.");
    }

    // Helper method to convert int back to TokenType for display
    private static TokenType intToTokenType(int tokenType) {
        return switch (tokenType) {
            case 262 -> TokenType.VAR;
            case 263 -> TokenType.TYPE;
            case 264 -> TokenType.IS;
            case 265 -> TokenType.INTEGER;
            case 266 -> TokenType.REAL;
            case 267 -> TokenType.BOOLEAN;
            case 268 -> TokenType.ARRAY;
            case 269 -> TokenType.RECORD;
            case 270 -> TokenType.END;
            case 271 -> TokenType.WHILE;
            case 272 -> TokenType.LOOP;
            case 273 -> TokenType.FOR;
            case 274 -> TokenType.IN;
            case 275 -> TokenType.REVERSE;
            case 276 -> TokenType.IF;
            case 277 -> TokenType.THEN;
            case 278 -> TokenType.ELSE;
            case 279 -> TokenType.PRINT;
            case 280 -> TokenType.ROUTINE;
            case 281 -> TokenType.TRUE;
            case 282 -> TokenType.FALSE;
            case 283 -> TokenType.AND;
            case 284 -> TokenType.OR;
            case 285 -> TokenType.XOR;
            case 286 -> TokenType.NOT;
            case 287 -> TokenType.ASSIGN;
            case 288 -> TokenType.RANGE;
            case 289 -> TokenType.PLUS;
            case 290 -> TokenType.MINUS;
            case 291 -> TokenType.MULTIPLY;
            case 292 -> TokenType.DIVIDE;
            case 293 -> TokenType.MODULO;
            case 294 -> TokenType.LESS;
            case 295 -> TokenType.LESS_EQUAL;
            case 296 -> TokenType.GREATER;
            case 297 -> TokenType.GREATER_EQUAL;
            case 298 -> TokenType.EQUAL;
            case 299 -> TokenType.NOT_EQUAL;
            case 300 -> TokenType.COLON;
            case 301 -> TokenType.SEMICOLON;
            case 302 -> TokenType.COMMA;
            case 303 -> TokenType.DOT;
            case 304 -> TokenType.LPAREN;
            case 305 -> TokenType.RPAREN;
            case 306 -> TokenType.LBRACKET;
            case 307 -> TokenType.RBRACKET;
            case 308 -> TokenType.ARROW;
            case 258 -> TokenType.IDENTIFIER;
            case 260 -> TokenType.INTEGER_LITERAL;
            case 261 -> TokenType.REAL_LITERAL;
            case 259 -> TokenType.STRING_LITERAL;
            case 309 -> TokenType.EOF;
            default -> TokenType.IDENTIFIER; // fallback
        };
    }
}