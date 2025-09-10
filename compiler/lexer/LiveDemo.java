import java.io.*;
import java.util.*;

/**
 * Interactive live demo for the Imperative (I) language lexer.
 * Perfect for presentations and demonstrations.
 */
public class LiveDemo {
    private static final String BANNER = 
        "╔══════════════════════════════════════════════════════════════════╗\n" +
        "║                    Team Hmm - Lexer Live Demo                    ║\n" +
        "║                   Imperative (I) Language                        ║\n" +
        "╚══════════════════════════════════════════════════════════════════╝";
    
    private static final String[] QUICK_EXAMPLES = {
        "var x: integer is 42;",
        "for i in 1..10 loop print i; end",
        "person.address.street := \"123 Main St\";",
        "var x @ 42;  // Error demo",
        "// Comment demo\nvar y: real;"
    };
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(BANNER);
        System.out.println();
        
        while (true) {
            showMenu();
            System.out.print("Your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1" -> runCustomInput(scanner);
                case "2" -> showQuickExamples(scanner);
                case "3" -> runFileInput(scanner);
                case "4" -> showTokenTypes();
                case "5" -> {
                    System.out.println("Thanks for using Team Hmm Lexer Demo!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.\n");
            }
        }
    }
    
    private static void showMenu() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("LEXER DEMO OPTIONS:");
        System.out.println("═══════════════════════════════════════");
        System.out.println("1. Enter custom source code");
        System.out.println("2. Quick examples");
        System.out.println("3. Load from file");
        System.out.println("4. Show all token types");
        System.out.println("5. Exit");
        System.out.println("═══════════════════════════════════════");
    }
    
    private static void runCustomInput(Scanner scanner) {
        System.out.println("\nCUSTOM INPUT MODE");
        System.out.println("Enter your Imperative (I) source code (type 'END' on a new line to finish):");
        System.out.println("───────────────────────────────────────");
        
        StringBuilder sourceCode = new StringBuilder();
        String line;
        
        while (!(line = scanner.nextLine()).equals("END")) {
            sourceCode.append(line).append("\n");
        }
        
        if (sourceCode.length() == 0) {
            System.out.println("No input provided.\n");
            return;
        }
        
        // Remove trailing newline
        String input = sourceCode.toString().trim();
        System.out.println("───────────────────────────────────────");
        analyzeInput("Custom Input", input);
    }
    
    private static void showQuickExamples(Scanner scanner) {
        System.out.println("\nQUICK EXAMPLES");
        System.out.println("Choose an example to analyze:");
        System.out.println("───────────────────────────────────────");
        
        for (int i = 0; i < QUICK_EXAMPLES.length; i++) {
            System.out.printf("%d. %s\n", i + 1, QUICK_EXAMPLES[i].replace("\n", "\\n"));
        }
        System.out.println("0. Back to main menu");
        System.out.println("───────────────────────────────────────");
        System.out.print("Example number: ");
        
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice == 0) return;
            if (choice < 1 || choice > QUICK_EXAMPLES.length) {
                System.out.println("Invalid example number.\n");
                return;
            }
            
            String example = QUICK_EXAMPLES[choice - 1];
            analyzeInput("Example " + choice, example);
            
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.\n");
        }
    }
    
    private static void runFileInput(Scanner scanner) {
        System.out.println("\nFILE INPUT MODE");
        System.out.print("Enter file path (relative to current directory): ");
        String filePath = scanner.nextLine().trim();
        
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            if (content.length() == 0) {
                System.out.println("File is empty or could not be read.\n");
                return;
            }
            
            analyzeInput("File: " + filePath, content.toString().trim());
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage() + "\n");
        }
    }
    
    private static void analyzeInput(String inputName, String sourceCode) {
        System.out.println("\nLEXICAL ANALYSIS RESULTS");
        System.out.printf("Input: %s\n", inputName);
        System.out.println("═══════════════════════════════════════");
        System.out.println("SOURCE CODE:");
        System.out.println("───────────────────────────────────────");
        
        // Show source with line numbers
        String[] lines = sourceCode.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("%2d│ %s\n", i + 1, lines[i]);
        }
        
        System.out.println("───────────────────────────────────────");
        System.out.println("RECOGNIZED TOKENS:");
        System.out.println("───────────────────────────────────────");
        
        try {
            Lexer lexer = new Lexer(new StringReader(sourceCode));
            List<Token> tokens = new ArrayList<>();
            Token token;
            int index = 0;
            
            // Collect all tokens
            while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
                tokens.add(token);
            }
            tokens.add(token); // Add EOF token
            
            // Display tokens in a formatted way
            for (Token t : tokens) {
                String typeColor = getTokenTypeColor(t.getType());
                System.out.printf("[%2d] %s%-15s%s : %-20s @ %d:%d\n",
                    index++, typeColor, t.getType(), ":",
                    "\"" + t.getLexeme() + "\"", t.getLine(), t.getColumn());
            }
            
            System.out.println("───────────────────────────────────────");
            System.out.printf("SUCCESS: %d tokens recognized\n\n", tokens.size());
            
        } catch (LexerException e) {
            System.out.println("LEXICAL ERROR:");
            System.out.printf("   %s\n", e.getMessage());
            System.out.printf("   Location: Line %d, Column %d\n", e.getLine(), e.getColumn());
            System.out.println("───────────────────────────────────────");
            System.out.println("This demonstrates error handling with precise location!\n");
        }
    }
    
    private static String getTokenTypeColor(TokenType type) {
        // Simple visual grouping for presentation
        return switch (type.toString()) {
            case "VAR", "TYPE", "IS", "ROUTINE", "END", "IF", "THEN", "ELSE",
                 "WHILE", "LOOP", "FOR", "IN", "REVERSE", "RETURN", "PRINT" -> "K"; // Keywords
            case "INTEGER", "REAL", "BOOLEAN", "ARRAY", "RECORD" -> "T"; // Types
            case "IDENTIFIER" -> "I"; // Identifiers
            case "INTEGER_LITERAL", "REAL_LITERAL", "STRING_LITERAL", "TRUE", "FALSE" -> "L"; // Literals
            case "PLUS", "MINUS", "MULTIPLY", "DIVIDE", "MODULO", "EQUAL", "NOT_EQUAL",
                 "LESS", "GREATER", "LESS_EQUAL", "GREATER_EQUAL", "AND", "OR", "XOR", "NOT" -> "O"; // Operators
            case "ASSIGN", "COLON", "SEMICOLON", "COMMA", "DOT", "RANGE" -> "P"; // Punctuation
            case "LPAREN", "RPAREN", "LBRACKET", "RBRACKET" -> "B"; // Brackets
            default -> "S"; // Others/EOF
        };
    }
    
    private static void showTokenTypes() {
        System.out.println("\nALL TOKEN TYPES (43 total)");
        System.out.println("═══════════════════════════════════════");
        
        System.out.println("KEYWORDS:");
        System.out.println("   VAR, TYPE, IS, ROUTINE, END, IF, THEN, ELSE");
        System.out.println("   WHILE, LOOP, FOR, IN, REVERSE, RETURN, PRINT");

        System.out.println("\nTYPE KEYWORDS:");
        System.out.println("   INTEGER, REAL, BOOLEAN, ARRAY, RECORD");

        System.out.println("\nIDENTIFIERS:");
        System.out.println("   IDENTIFIER (user-defined names)");

        System.out.println("\nLITERALS:");
        System.out.println("   INTEGER_LITERAL, REAL_LITERAL, STRING_LITERAL");
        System.out.println("   TRUE, FALSE");

        System.out.println("\nOPERATORS:");
        System.out.println("   PLUS(+), MINUS(-), MULTIPLY(*), DIVIDE(/), MODULO(%)");
        System.out.println("   EQUAL(=), NOT_EQUAL(/=), LESS(<), GREATER(>)");
        System.out.println("   LESS_EQUAL(<=), GREATER_EQUAL(>=)");
        System.out.println("   AND, OR, XOR, NOT");

        System.out.println("\nPUNCTUATION:");
        System.out.println("   ASSIGN(:=), COLON(:), SEMICOLON(;), COMMA(,)");
        System.out.println("   DOT(.), RANGE(..)");

        System.out.println("\nBRACKETS:");
        System.out.println("   LPAREN((), RPAREN()), LBRACKET([), RBRACKET(])");

        System.out.println("\nSPECIAL:");
        System.out.println("   EOF (End of File)");
        
        System.out.println("═══════════════════════════════════════\n");
    }
}
