package compiler.parser;

import java.util.List;

/**
 * Represents a complete program consisting of a list of declarations and statements.
 */
public class Program implements ASTNode {
    public final List<ASTNode> nodes;

    public Program(List<ASTNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Program:\n");
        for (ASTNode node : nodes) {
            sb.append("  ").append(node.toString().replace("\n", "\n  ")).append("\n");
        }
        return sb.toString().trim();
    }
}