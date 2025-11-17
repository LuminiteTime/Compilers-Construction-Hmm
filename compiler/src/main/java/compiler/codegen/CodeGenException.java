package compiler.codegen;

/**
 * Exception thrown during code generation
 */
public class CodeGenException extends RuntimeException {
    public CodeGenException(String message) {
        super(message);
    }

    public CodeGenException(String message, Throwable cause) {
        super(message, cause);
    }
}

