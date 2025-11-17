package compiler.codegen;

/**
 * Maps language operators to WebAssembly instructions
 */
public class WasmOperator {

    /**
     * Get WASM instruction for binary operator
     */
    public static String getBinaryOp(String operator, String operandType) {
        return switch (operator) {
            // Arithmetic
            case "+" -> operandType.equals("f64") ? "f64.add" : "i32.add";
            case "-" -> operandType.equals("f64") ? "f64.sub" : "i32.sub";
            case "*" -> operandType.equals("f64") ? "f64.mul" : "i32.mul";
            case "/" -> operandType.equals("f64") ? "f64.div" : "i32.div_s";
            case "mod" -> "i32.rem_s";

            // Comparison (signed)
            case "<" -> operandType.equals("f64") ? "f64.lt" : "i32.lt_s";
            case "<=" -> operandType.equals("f64") ? "f64.le" : "i32.le_s";
            case ">" -> operandType.equals("f64") ? "f64.gt" : "i32.gt_s";
            case ">=" -> operandType.equals("f64") ? "f64.ge" : "i32.ge_s";
            case "=" -> operandType.equals("f64") ? "f64.eq" : "i32.eq";
            case "/=" -> operandType.equals("f64") ? "f64.ne" : "i32.ne";

            // Logical (bitwise on i32)
            case "and" -> "i32.and";
            case "or" -> "i32.or";
            case "xor" -> "i32.xor";

            default -> throw new CodeGenException("Unknown binary operator: " + operator);
        };
    }

    /**
     * Get WASM instruction for unary operator
     */
    public static String getUnaryOp(String operator, String operandType) {
        return switch (operator) {
            case "-" -> operandType.equals("f64") ? "f64.neg" : "i32.const 0\ni32.sub"; // 0 - x
            case "+" -> "";  // identity, no operation
            case "not" -> "i32.eqz";
            default -> throw new CodeGenException("Unknown unary operator: " + operator);
        };
    }

    /**
     * Check if operator is comparison
     */
    public static boolean isComparisonOp(String operator) {
        return operator.equals("<") || operator.equals("<=") ||
               operator.equals(">") || operator.equals(">=") ||
               operator.equals("=") || operator.equals("/=");
    }

    /**
     * Check if operator is logical
     */
    public static boolean isLogicalOp(String operator) {
        return operator.equals("and") || operator.equals("or") ||
               operator.equals("xor") || operator.equals("not");
    }

    /**
     * Check if operator is arithmetic
     */
    public static boolean isArithmeticOp(String operator) {
        return operator.equals("+") || operator.equals("-") ||
               operator.equals("*") || operator.equals("/") ||
               operator.equals("mod");
    }
}

