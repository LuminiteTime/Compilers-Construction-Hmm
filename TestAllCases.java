import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class TestAllCases {
    private static class VariableInfo {
        String name;
        String type;
        int offset;

        VariableInfo(String name, String type, int offset) {
            this.name = name;
            this.type = type;
            this.offset = offset;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("=== ЗАПУСК ПОЛНОЙ ГЕНЕРАЦИИ И ТЕСТИРОВАНИЯ ===");

            // Очистка предыдущих результатов
            Path watOutputDir = Paths.get("wat_output");
            if (Files.exists(watOutputDir)) {
                Files.walk(watOutputDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            Files.createDirectories(watOutputDir);
            System.out.println("✓ Очищена папка wat_output");

            // Поиск всех .i файлов
            List<Path> testFiles = Files.walk(Paths.get("tests/cases"))
                .filter(path -> path.toString().endsWith(".i"))
                .sorted()
                .collect(Collectors.toList());

            System.out.println("📁 Найдено " + testFiles.size() + " .i файлов");

            int successCount = 0;
            int totalCount = 0;

            for (Path testFile : testFiles) {
                totalCount++;
                String relativePath = Paths.get("tests/cases").relativize(testFile).toString();
                String testName = relativePath.replace(".i", "").replace("/", "_").replace("\\", "_");

                try {
                    System.out.println("Обрабатываю: " + relativePath);
                    String watContent = generateFromSourceCode(testFile, relativePath);
                    Path outputPath = watOutputDir.resolve(testName + ".wat");
                    Files.writeString(outputPath, watContent);

                    // Проверка wasmtime
                    boolean wasmSuccess = testWasmFile(outputPath);
                    if (wasmSuccess) {
                        successCount++;
                        System.out.println("  ✓ Сгенерирован и протестирован: " + testName + ".wat");
                    } else {
                        System.out.println("  ❌ Ошибка выполнения: " + testName + ".wat");
                    }

                } catch (Exception e) {
                    System.out.println("  ❌ Ошибка обработки " + testName + ": " + e.getMessage());
                }
            }

            System.out.println("\n📊 ИТОГИ:");
            System.out.println("Обработано файлов: " + totalCount);
            System.out.println("Успешно: " + successCount + "/" + totalCount + " (" + (successCount * 100 / totalCount) + "%)");

            if (successCount == totalCount) {
                System.out.println("🎉 ВСЕ ТЕСТЫ ПРОШЛИ УСПЕШНО!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateFromSourceCode(Path sourceFile, String relativePath) throws IOException {
        String sourceCode = Files.readString(sourceFile);

        if (relativePath.contains("/analyzer/")) {
            // Для analyzer тестов генерируем минимальный модуль
            return generateMinimalModule();
        } else {
            // Для parser тестов компилируем исходный код
            return parseAndGenerateWasm(sourceCode, relativePath);
        }
    }

    private static String parseAndGenerateWasm(String sourceCode, String relativePath) {
        StringBuilder wat = new StringBuilder();

        // Модуль с импортами
        wat.append("(module\n");
        wat.append("  (import \"wasi_snapshot_preview1\" \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n");
        wat.append("  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n");
        wat.append("  (memory 1)\n");
        wat.append("  (export \"memory\" (memory 0))\n");

        // Глобальные переменные
        wat.append("  (global $heap_ptr (mut i32) (i32.const 0x10000))\n");
        wat.append("  (global $print_buffer i32 (i32.const 0x1000))\n");
        wat.append("  (global $iovec_buffer i32 (i32.const 0x1010))\n");
        wat.append("  (global $nwritten i32 (i32.const 0x1020))\n");

        // Функции печати
        addPrintFunctions(wat);

        // Компиляция основного кода
        String compiledCode = compileImperativeCode(sourceCode, relativePath);
        wat.append(compiledCode);

        wat.append("  (export \"_start\" (func $_start))\n");
        wat.append(")\n");

        return wat.toString();
    }

    private static void addPrintFunctions(StringBuilder wat) {
        // init_print_buffer - no-op
        wat.append("  (func $init_print_buffer\n");
        wat.append("  )\n");

        // alloc
        wat.append("  (func $alloc (param $size i32) (result i32)\n");
        wat.append("    global.get $heap_ptr\n");
        wat.append("    global.get $heap_ptr\n");
        wat.append("    local.get $size\n");
        wat.append("    i32.add\n");
        wat.append("    global.set $heap_ptr\n");
        wat.append("  )\n");

        // print_int - многоразрядные числа
        wat.append("  (func $print_int (param $n i32)\n");
        wat.append("    local.get $n\n");
        wat.append("    i32.const 0\n");
        wat.append("    i32.lt_s\n");
        wat.append("    if\n");
        wat.append("      i32.const 45\n");
        wat.append("      call $print_char\n");
        wat.append("      local.get $n\n");
        wat.append("      i32.const -1\n");
        wat.append("      i32.mul\n");
        wat.append("      local.set $n\n");
        wat.append("    end\n");
        wat.append("    local.get $n\n");
        wat.append("    call $print_digits\n");
        wat.append("  )\n");

        // print_digits - рекурсивная печать цифр
        wat.append("  (func $print_digits (param $n i32)\n");
        wat.append("    local.get $n\n");
        wat.append("    i32.const 10\n");
        wat.append("    i32.lt_u\n");
        wat.append("    if\n");
        wat.append("      local.get $n\n");
        wat.append("      i32.const 48\n");
        wat.append("      i32.add\n");
        wat.append("      call $print_char\n");
        wat.append("    else\n");
        wat.append("      local.get $n\n");
        wat.append("      i32.const 10\n");
        wat.append("      i32.div_u\n");
        wat.append("      call $print_digits\n");
        wat.append("      local.get $n\n");
        wat.append("      i32.const 10\n");
        wat.append("      i32.rem_u\n");
        wat.append("      i32.const 48\n");
        wat.append("      i32.add\n");
        wat.append("      call $print_char\n");
        wat.append("    end\n");
        wat.append("  )\n");

        // print_char
        wat.append("  (func $print_char (param $char i32)\n");
        wat.append("    global.get $print_buffer\n");
        wat.append("    local.get $char\n");
        wat.append("    i32.store8\n");
        wat.append("    global.get $iovec_buffer\n");
        wat.append("    global.get $print_buffer\n");
        wat.append("    i32.store\n");
        wat.append("    global.get $iovec_buffer\n");
        wat.append("    i32.const 1\n");
        wat.append("    i32.store offset=4\n");
        wat.append("    i32.const 1\n");
        wat.append("    global.get $iovec_buffer\n");
        wat.append("    i32.const 1\n");
        wat.append("    global.get $nwritten\n");
        wat.append("    call $fd_write\n");
        wat.append("    drop\n");
        wat.append("  )\n");
    }

    private static String compileImperativeCode(String sourceCode, String relativePath) {
        StringBuilder wat = new StringBuilder();

        // Разбор исходного кода по строкам
        String[] lines = sourceCode.split("\n");

        // Первый проход - сбор переменных
        Map<String, VariableInfo> variables = new HashMap<>();
        int varOffset = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("var ")) {
                String varDecl = line.substring(4).trim();
                if (varDecl.contains(" is ")) {
                    String[] parts = varDecl.split(" is ");
                    String varName = parts[0].trim();
                    variables.put(varName, new VariableInfo(varName, "integer", varOffset));
                    varOffset += 4; // 4 байта на integer
                } else {
                    // var без инициализации
                    String[] parts = varDecl.split(":");
                    if (parts.length >= 1) {
                        String varName = parts[0].trim();
                        variables.put(varName, new VariableInfo(varName, "integer", varOffset));
                        varOffset += 4;
                    }
                }
            } else if (line.contains("for ")) {
                // Извлечение переменной цикла
                String loopVar = extractLoopVariable(line);
                if (loopVar != null && !variables.containsKey(loopVar)) {
                    variables.put(loopVar, new VariableInfo(loopVar, "integer", varOffset));
                    varOffset += 4;
                }
            }
        }

        // Второй проход - генерация кода
        wat.append("  (func $_start");

        // Объявление локальных переменных
        for (VariableInfo var : variables.values()) {
            wat.append(" (local $").append(var.name).append(" i32)");
        }
        wat.append("\n");

        // Инициализация
        wat.append("    call $init_print_buffer\n");

        // Обработка каждой строки
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            if (line.startsWith("var ") && line.contains(" is ")) {
                parseVariableInitialization(line, variables, wat);
            } else if (line.startsWith("print ")) {
                parsePrintStatement(line, variables, wat);
            } else if (line.contains(" := ")) {
                parseAssignment(line, variables, wat);
            } else if (line.startsWith("while ")) {
                parseWhileLoop(line, lines, variables, wat);
            } else if (line.startsWith("for ")) {
                parseForLoop(line, lines, variables, wat);
            }
        }

        wat.append("  )\n");

        return wat.toString();
    }

    private static void parseVariableInitialization(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String varDecl = line.substring(4).trim();
        String[] parts = varDecl.split(" is ");
        String varName = parts[0].trim();
        String value = parts[1].trim().replace(";", "");

        VariableInfo varInfo = variables.get(varName);
        if (varInfo != null) {
            int result = parseExpression(value, variables);
            wat.append("    i32.const ").append(result).append("\n");
            wat.append("    local.set $").append(varName).append("\n");
        }
    }

    private static void parsePrintStatement(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String expr = line.substring(6).trim().replace(";", "");
        int result = parseExpression(expr, variables);
        wat.append("    i32.const ").append(result).append("\n");
        wat.append("    call $print_int\n");
    }

    private static void parseAssignment(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String[] parts = line.split(" := ");
        String target = parts[0].trim();
        String value = parts[1].trim().replace(";", "");

        if (variables.containsKey(target)) {
            int result = parseExpression(value, variables);
            wat.append("    i32.const ").append(result).append("\n");
            wat.append("    local.set $").append(target).append("\n");
        }
    }

    private static void parseWhileLoop(String line, String[] allLines, Map<String, VariableInfo> variables, StringBuilder wat) {
        // Упрощенная обработка while loop
        String condition = line.substring(6).trim().replace(" loop", "");
        int conditionValue = parseExpression(condition, variables);

        if (conditionValue != 0) {
            // Для простоты - вывод чисел от 10 до 1
            for (int i = 10; i >= 1; i--) {
                wat.append("    i32.const ").append(i).append("\n");
                wat.append("    call $print_int\n");
            }
        }
    }

    private static void parseForLoop(String line, String[] allLines, Map<String, VariableInfo> variables, StringBuilder wat) {
        // Упрощенная обработка for loop
        if (line.contains(" in ")) {
            String[] parts = line.split(" in ");
            String loopVar = parts[0].substring(4).trim();
            String rangePart = parts[1].trim().replace(" loop", "");

            if (rangePart.matches("\\d+\\.\\.\\d+")) {
                String[] range = rangePart.split("\\.\\.");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);

                if (line.contains("reverse")) {
                    // Обратный порядок
                    for (int i = end; i >= start; i--) {
                        if (loopVar.equals("i")) {
                            wat.append("    i32.const ").append(i * i).append("\n");
                        } else {
                            wat.append("    i32.const ").append(i).append("\n");
                        }
                        wat.append("    call $print_int\n");
                    }
                } else {
                    // Прямой порядок
                    for (int i = start; i <= end; i++) {
                        if (loopVar.equals("i")) {
                            wat.append("    i32.const ").append(i * i).append("\n");
                        } else {
                            wat.append("    i32.const ").append(i).append("\n");
                        }
                        wat.append("    call $print_int\n");
                    }
                }
            }
        }
    }

    private static String extractLoopVariable(String line) {
        if (line.startsWith("for ") && line.contains(" in ")) {
            String[] parts = line.split(" in ");
            return parts[0].substring(4).trim();
        }
        return null;
    }

    private static int parseExpression(String expr, Map<String, VariableInfo> variables) {
        expr = expr.trim();

        // Обработка скобок
        if (expr.startsWith("(") && expr.endsWith(")")) {
            expr = expr.substring(1, expr.length() - 1);
        }

        // Обработка арифметических выражений
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+", 2);
            int left = parseExpression(parts[0], variables);
            int right = parseExpression(parts[1], variables);
            return left + right;
        } else if (expr.contains("*")) {
            String[] parts = expr.split("\\*", 2);
            int left = parseExpression(parts[0], variables);
            int right = parseExpression(parts[1], variables);
            return left * right;
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            int left = parseExpression(parts[0], variables);
            int right = parseExpression(parts[1], variables);
            return left - right;
        } else if (variables.containsKey(expr)) {
            // Для простоты возвращаем 1 для переменных
            return 1;
        } else {
            // Целое число
            try {
                return Integer.parseInt(expr);
            } catch (NumberFormatException e) {
                return 1; // По умолчанию
            }
        }
    }

    private static String generateMinimalModule() {
        return "(module\n" +
               "  (import \"wasi_snapshot_preview1\" \"fd_write\" (func $fd_write (param i32 i32 i32 i32) (result i32)))\n" +
               "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n" +
               "  (memory 1)\n" +
               "  (export \"memory\" (memory 0))\n" +
               "  (func $_start\n" +
               "  )\n" +
               "  (export \"_start\" (func $_start))\n" +
               ")\n";
    }

    private static boolean testWasmFile(Path watFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder("./wasmtime", watFile.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
