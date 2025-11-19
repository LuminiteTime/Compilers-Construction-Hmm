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
            System.out.println("=== ЗАПУСК ГЕНЕРАЦИИ WAT ФАЙЛОВ ===");

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

            // Список тестов для генерации WAT
            String[] testsToCompile = {
                "analyzer/arrays/array_checks",
                "analyzer/control_flow/const_and_control",
                "analyzer/optimizer/remove_unused_decl",
                "analyzer/precedence/precedence_arith",
                "analyzer/precedence/precedence_boolean",
                "analyzer/precedence/precedence_mixed",
                "analyzer/precedence/precedence_unary",
                "analyzer/print/print_multiple",
                "analyzer/records/field_nonrecord"
            };

            int successCount = 0;

            for (String testPath : testsToCompile) {
                try {
                    Path sourceFile = Paths.get("tests/cases", testPath + ".i");
                    if (!Files.exists(sourceFile)) {
                        System.out.println("✗ Файл не найден: " + sourceFile);
                        continue;
                    }

                    String relativePath = testPath;
                    String testName = testPath.replace("/", "_");

                    System.out.println("Обрабатываю: " + relativePath);
                    String watContent = parseAndGenerateWasm(Files.readString(sourceFile), relativePath);
                    Path outputPath = watOutputDir.resolve(testName + ".wat");
                    Files.writeString(outputPath, watContent);

                    successCount++;
                    System.out.println("  ✓ Сгенерирован: " + testName + ".wat");

                } catch (Exception e) {
                    System.out.println("  ✗ Ошибка обработки " + testPath + ": " + e.getMessage());
                }
            }

            System.out.println("\n📊 Сгенерировано " + successCount + " WAT файлов");

        } catch (Exception e) {
            e.printStackTrace();
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
                    String varNameWithType = parts[0].trim();
                    // Извлекаем имя и тип переменной
                    String varName = varNameWithType.split(":")[0].trim();
                    String varType = "integer"; // default
                    if (varNameWithType.contains(":")) {
                        String typePart = varNameWithType.split(":")[1].trim();
                        if (typePart.startsWith("real")) {
                            varType = "real";
                        } else if (typePart.startsWith("boolean")) {
                            varType = "boolean";
                        }
                        // Для array типов - используем integer как базовый
                    }
                    variables.put(varName, new VariableInfo(varName, varType, varOffset));
                    varOffset += 4; // 4 байта на все типы
                } else {
                    // var без инициализации
                    String[] parts = varDecl.split(":");
                    if (parts.length >= 1) {
                        String varName = parts[0].trim();
                        variables.put(varName, new VariableInfo(varName, "integer", varOffset));
                        varOffset += 4;
                    }
                }
            }
        }

        // Второй проход - генерация кода
        wat.append("  (func $_start");

        // Объявление локальных переменных
        for (VariableInfo var : variables.values()) {
            String wasmType = "i32"; // default
            if ("real".equals(var.type)) {
                wasmType = "f32";
            } else if ("boolean".equals(var.type)) {
                wasmType = "i32"; // boolean as i32
            }
            wat.append(" (local $").append(var.name).append(" ").append(wasmType).append(")");
        }
        wat.append("\n");

        // Инициализация
        wat.append("    call $init_print_buffer\n");

        // Обработка каждой строки
        int currentLine = 0;
        while (currentLine < lines.length) {
            String line = lines[currentLine].trim();
            if (line.isEmpty() || line.startsWith("//")) {
                currentLine++;
                continue;
            }

            if (line.startsWith("var ") && line.contains(" is ")) {
                parseVariableInitialization(line, variables, wat);
                currentLine++;
            } else if (line.startsWith("print ")) {
                parsePrintStatement(line, variables, wat);
                currentLine++;
            } else if (line.contains(" := ")) {
                parseAssignment(line, variables, wat);
                currentLine++;
            } else {
                currentLine++;
            }
        }

        wat.append("  )\n");

        return wat.toString();
    }

    private static void parseVariableInitialization(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String varDecl = line.substring(4).trim();
        String[] parts = varDecl.split(" is ");
        String varNameWithType = parts[0].trim();
        String varName = varNameWithType.split(":")[0].trim();
        String value = parts[1].trim().replace(";", "");

        VariableInfo varInfo = variables.get(varName);
        if (varInfo != null) {
            generateExpressionCode(value, variables, wat);
            wat.append("    local.set $").append(varName).append("\n");
        }
    }

    private static void parsePrintStatement(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String expr = line.substring(6).trim().replace(";", "");
        generateExpressionCode(expr, variables, wat);
        wat.append("    call $print_int\n");
    }

    private static void parseAssignment(String line, Map<String, VariableInfo> variables, StringBuilder wat) {
        String[] parts = line.split(" := ");
        String target = parts[0].trim();
        String value = parts[1].trim().replace(";", "");

        if (variables.containsKey(target)) {
            generateExpressionCode(value, variables, wat);
            wat.append("    local.set $").append(target).append("\n");
        }
    }

    private static void generateExpressionCode(String expr, Map<String, VariableInfo> variables, StringBuilder wat) {
        expr = expr.trim();

        // Обработка сравнений
        if (expr.contains(" > ")) {
            String[] parts = expr.split(" > ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.gt_s\n");
            return;
        } else if (expr.contains(" < ")) {
            String[] parts = expr.split(" < ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.lt_s\n");
            return;
        } else if (expr.contains(" >= ")) {
            String[] parts = expr.split(" >= ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.ge_s\n");
            return;
        } else if (expr.contains(" <= ")) {
            String[] parts = expr.split(" <= ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.le_s\n");
            return;
        } else if (expr.contains(" == ")) {
            String[] parts = expr.split(" == ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.eq\n");
            return;
        } else if (expr.contains(" != ")) {
            String[] parts = expr.split(" != ", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.ne\n");
            return;
        }

        // Обработка арифметических выражений
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.mul\n");
            return;
        } else if (expr.contains("+")) {
            String[] parts = expr.split("\\+", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.add\n");
            return;
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            generateExpressionCode(parts[0], variables, wat);
            generateExpressionCode(parts[1], variables, wat);
            wat.append("    i32.sub\n");
            return;
        } else if (variables.containsKey(expr)) {
            wat.append("    local.get $").append(expr).append("\n");
            return;
        } else {
            // Целое число
            try {
                int value = Integer.parseInt(expr);
                wat.append("    i32.const ").append(value).append("\n");
                return;
            } catch (NumberFormatException e) {
                // Real число
                try {
                    float value = Float.parseFloat(expr);
                    wat.append("    f32.const ").append(value).append("\n");
                    return;
                } catch (NumberFormatException e2) {
                    // По умолчанию
                    wat.append("    i32.const 0\n");
                    return;
                }
            }
        }
    }

    private static int parseExpression(String expr, Map<String, VariableInfo> variables) {
        expr = expr.trim();

        // Обработка арифметических выражений
        if (expr.contains("*")) {
            String[] parts = expr.split("\\*", 2);
            int left = parseExpression(parts[0], variables);
            int right = parseExpression(parts[1], variables);
            return left * right;
        } else if (expr.contains("+")) {
            String[] parts = expr.split("\\+", 2);
            int left = parseExpression(parts[0], variables);
            int right = parseExpression(parts[1], variables);
            return left + right;
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            return parseExpression(parts[0], variables) - parseExpression(parts[1], variables);
        } else if (variables.containsKey(expr)) {
            return 1;
        } else {
            try {
                return Integer.parseInt(expr);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
    }
}
