package compiler.codegen;

import java.util.*;

/**
 * Utility functions for code generation
 */
public class CodeGenUtils {

    /**
     * Convert language type to WASM type string
     */
    public static String toWasmType(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer" -> "i32";
            case "real" -> "f64";
            case "boolean" -> "i32";  // 0 = false, 1 = true
            case "array", "record" -> "i32";  // pointer to memory
            default -> throw new CodeGenException("Unknown language type: " + langType);
        };
    }

    /**
     * Check if type is numeric (can do arithmetic)
     */
    public static boolean isNumericType(String type) {
        String t = type.toLowerCase();
        return t.equals("integer") || t.equals("real");
    }

    /**
     * Check if type is boolean
     */
    public static boolean isBooleanType(String type) {
        return type.toLowerCase().equals("boolean");
    }

    /**
     * Check if type is array
     */
    public static boolean isArrayType(String type) {
        return type.toLowerCase().startsWith("array");
    }

    /**
     * Check if type is record
     */
    public static boolean isRecordType(String type) {
        return type.toLowerCase().equals("record");
    }

    /**
     * Get numeric result type for binary operation
     */
    public static String getNumericResultType(String leftType, String rightType) {
        if (leftType.equalsIgnoreCase("real") || rightType.equalsIgnoreCase("real")) {
            return "real";
        }
        if (leftType.equalsIgnoreCase("integer") && rightType.equalsIgnoreCase("integer")) {
            return "integer";
        }
        throw new CodeGenException("Cannot perform arithmetic on types: " + leftType + ", " + rightType);
    }

    /**
     * Generate unique label name
     */
    private static int labelCounter = 0;
    public static synchronized String generateLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    /**
     * Generate unique variable name
     */
    private static int varCounter = 0;
    public static synchronized String generateTempVar(String prefix) {
        return prefix + "_" + (varCounter++);
    }

    /**
     * Escape identifier for WASM
     */
    public static String escapeIdentifier(String name) {
        // Replace invalid characters with underscores
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Check if identifier is valid WASM name
     */
    public static boolean isValidIdentifier(String name) {
        return name != null && name.length() > 0 &&
               name.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * Convert integer to WASM i32 format
     */
    public static String formatI32Const(int value) {
        return "i32.const " + value;
    }

    /**
     * Convert float to WASM f64 format
     */
    public static String formatF64Const(double value) {
        return "f64.const " + value;
    }

    /**
     * Generate type conversion instruction if needed
     */
    public static String generateTypeConversion(String fromType, String toType) {
        if (fromType.equalsIgnoreCase(toType)) {
            return "";  // No conversion
        }

        if (fromType.equalsIgnoreCase("integer") && toType.equalsIgnoreCase("real")) {
            return "f64.convert_i32_s";
        } else if (fromType.equalsIgnoreCase("real") && toType.equalsIgnoreCase("integer")) {
            return "i32.trunc_f64_s";
        } else if (fromType.equalsIgnoreCase("integer") && toType.equalsIgnoreCase("boolean")) {
            return "i32.const 0\ni32.ne";  // non-zero = true
        }

        throw new CodeGenException("Cannot convert " + fromType + " to " + toType);
    }

    /**
     * Validate identifier for use in WASM
     */
    public static String validateIdentifier(String name) {
        if (!isValidIdentifier(name)) {
            throw new CodeGenException("Invalid identifier: " + name);
        }
        return name;
    }

    /**
     * Format memory offset calculation
     */
    public static String formatMemoryOffset(int offset) {
        if (offset == 0) {
            return "";
        }
        return "i32.const " + offset + "\ni32.add";
    }

    /**
     * Get size of array element in bytes
     */
    public static int getArrayElementSize(String elementType) {
        return switch (elementType.toLowerCase()) {
            case "integer", "boolean" -> 4;
            case "real" -> 8;
            case "array", "record" -> 4;  // pointer
            default -> throw new CodeGenException("Unknown element type: " + elementType);
        };
    }

    /**
     * Check if operator requires special handling
     */
    public static boolean requiresSpecialHandling(String operator) {
        return operator.equals("/") || operator.equals("mod") ||
               operator.equals("and") || operator.equals("or");
    }

    /**
     * Format comment line
     */
    public static String comment(String text) {
        return ";; " + text;
    }

    /**
     * Pad string to length with spaces (for formatting)
     */
    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    /**
     * Print debug information
     */
    public static void debug(String message) {
        if (System.getenv("DEBUG_CODEGEN") != null) {
            System.err.println("[CodeGen] " + message);
        }
    }
}

