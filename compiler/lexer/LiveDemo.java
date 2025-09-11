import java.io.*;
import java.util.*;

public class LiveDemo {
    private static final String[] QUICK_EXAMPLES = {
        "var x: integer is 42;",
        "for i in 1..10 loop print i; end",
        "person.address.street := \"123 Main St\";",
        "var x @ 42;  // Error demo",
        "// Comment demo\nvar y: real;"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            showMenu();
            System.out.print("Your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> runCustomInput(scanner);
                case "2" -> showQuickExamples(scanner);
                case "3" -> runFileInput(scanner);
                case "4" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.\n");
            }
        }
    }

    private static void showMenu() {
        System.out.println("=======================================");
        System.out.println("LEXER DEMO OPTIONS:");
        System.out.println("=======================================");
        System.out.println("1. Enter custom source code");
        System.out.println("2. Quick examples");
        System.out.println("3. Load from file");
        System.out.println("4. Exit");
        System.out.println("=======================================");
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
        System.out.println("\n=== LEXICAL ANALYSIS ===");
        System.out.printf("Input: %s\n", inputName);
        System.out.println("Source code:");
        System.out.println("-------------------");

        String[] lines = sourceCode.split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf("%2d: %s\n", i + 1, lines[i]);
        }

        System.out.println("\nTokens:");
        System.out.println("-------------------");

        try {
            Lexer lexer = new Lexer(new StringReader(sourceCode));
            List<Token> tokens = new ArrayList<>();
            Token token;

            while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
                tokens.add(token);
            }
            tokens.add(token);
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                System.out.printf("[%d] %-15s : \"%s\" @ %d:%d\n",
                    i, t.getType(), t.getLexeme(), t.getLine(), t.getColumn());
            }

            System.out.println("-------------------");
            System.out.printf("Total: %d tokens\n\n", tokens.size());

        } catch (LexerException e) {
            System.out.println("ERROR:");
            System.out.printf("  %s\n", e.getMessage());
            System.out.printf("  Location: Line %d, Column %d\n\n", e.getLine(), e.getColumn());
        }
    }
    
}
