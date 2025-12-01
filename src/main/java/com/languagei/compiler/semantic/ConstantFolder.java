package com.languagei.compiler.semantic;

import com.languagei.compiler.ast.*;
import com.languagei.compiler.lexer.Position;

/**
 * Performs constant folding on the AST.
 */
public class ConstantFolder {

    public ProgramNode optimize(ProgramNode root) {
        return (ProgramNode) fold(root);
    }

    private ASTNode fold(ASTNode node) {
        if (node == null) return null;

        if (node instanceof ProgramNode) {
            ProgramNode prog = (ProgramNode) node;
            ProgramNode result = new ProgramNode(prog.getPosition());
            for (ASTNode decl : prog.getDeclarations()) {
                result.addDeclaration(fold(decl));
            }
            for (ASTNode stmt : prog.getStatements()) {
                result.addStatement(fold(stmt));
            }
            return result;
        }

        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            BlockNode result = new BlockNode(block.getPosition());
            for (ASTNode stmt : block.getStatements()) {
                result.addStatement(fold(stmt));
            }
            return result;
        }

        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode v = (VariableDeclarationNode) node;
            ASTNode type = v.getType(); // types are not folded here
            ASTNode init = fold(v.getInitializer());
            return new VariableDeclarationNode(v.getPosition(), v.getName(), type, init);
        }

        if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            ASTNode target = fold(a.getTarget());
            ASTNode value = fold(a.getValue());
            return new AssignmentNode(a.getPosition(), target, value);
        }

        if (node instanceof PrintStatementNode) {
            PrintStatementNode p = (PrintStatementNode) node;
            PrintStatementNode result = new PrintStatementNode(p.getPosition());
            for (ASTNode expr : p.getExpressions()) {
                result.addExpression(fold(expr));
            }
            return result;
        }

        if (node instanceof IfStatementNode) {
            IfStatementNode i = (IfStatementNode) node;
            ASTNode cond = fold(i.getCondition());
            BlockNode thenBlock = (BlockNode) fold(i.getThenBlock());
            BlockNode elseBlock = i.getElseBlock() != null ? (BlockNode) fold(i.getElseBlock()) : null;
            return new IfStatementNode(i.getPosition(), cond, thenBlock, elseBlock);
        }

        if (node instanceof WhileLoopNode) {
            WhileLoopNode w = (WhileLoopNode) node;
            ASTNode cond = fold(w.getCondition());
            BlockNode body = (BlockNode) fold(w.getBody());
            return new WhileLoopNode(w.getPosition(), cond, body);
        }

        if (node instanceof ReturnStatementNode) {
            ReturnStatementNode r = (ReturnStatementNode) node;
            ASTNode value = r.getValue() != null ? fold(r.getValue()) : null;
            return new ReturnStatementNode(r.getPosition(), value);
        }

        if (node instanceof RoutineDeclarationNode) {
            RoutineDeclarationNode r = (RoutineDeclarationNode) node;
            BlockNode body = r.getBody() != null ? (BlockNode) fold(r.getBody()) : null;
            return new RoutineDeclarationNode(r.getPosition(), r.getName(), r.getParameters(), r.getReturnType(), body);
        }

        if (node instanceof RoutineCallNode) {
            RoutineCallNode call = (RoutineCallNode) node;
            RoutineCallNode result = new RoutineCallNode(call.getPosition(), call.getName());
            for (ASTNode arg : call.getArguments()) {
                result.addArgument(fold(arg));
            }
            return result;
        }

        if (node instanceof ArrayAccessNode) {
            ArrayAccessNode a = (ArrayAccessNode) node;
            ASTNode array = fold(a.getArray());
            ASTNode index = fold(a.getIndex());
            return new ArrayAccessNode(a.getPosition(), array, index);
        }

        if (node instanceof RecordAccessNode) {
            RecordAccessNode r = (RecordAccessNode) node;
            ASTNode obj = fold(r.getObject());
            return new RecordAccessNode(r.getPosition(), obj, r.getFieldName());
        }

        if (node instanceof UnaryExpressionNode) {
            return foldUnary((UnaryExpressionNode) node);
        }

        if (node instanceof BinaryExpressionNode) {
            return foldBinary((BinaryExpressionNode) node);
        }

        // All other nodes (identifiers, literals, types, etc.) are returned as-is
        return node;
    }

    private ASTNode foldUnary(UnaryExpressionNode node) {
        ASTNode foldedOperand = fold(node.getOperand());
        if (!(foldedOperand instanceof LiteralNode)) {
            return new UnaryExpressionNode(node.getPosition(), node.getOperator(), foldedOperand);
        }

        LiteralNode lit = (LiteralNode) foldedOperand;
        Object v = lit.getValue();
        Position pos = node.getPosition();

        switch (node.getOperator()) {
            case PLUS:
                // +x => x for numeric literals
                if (v instanceof Number) return new LiteralNode(pos, v);
                return new UnaryExpressionNode(pos, node.getOperator(), foldedOperand);
            case MINUS:
                if (v instanceof Integer || v instanceof Long) {
                    long val = ((Number) v).longValue();
                    return new LiteralNode(pos, Long.valueOf(-val));
                }
                if (v instanceof Double || v instanceof Float) {
                    double val = ((Number) v).doubleValue();
                    return new LiteralNode(pos, Double.valueOf(-val));
                }
                return new UnaryExpressionNode(pos, node.getOperator(), foldedOperand);
            case NOT:
                if (v instanceof Boolean) {
                    return new LiteralNode(pos, !((Boolean) v));
                }
                return new UnaryExpressionNode(pos, node.getOperator(), foldedOperand);
            default:
                return new UnaryExpressionNode(pos, node.getOperator(), foldedOperand);
        }
    }

    private ASTNode foldBinary(BinaryExpressionNode node) {
        ASTNode leftFolded = fold(node.getLeft());
        ASTNode rightFolded = fold(node.getRight());

        if (!(leftFolded instanceof LiteralNode) || !(rightFolded instanceof LiteralNode)) {
            return new BinaryExpressionNode(node.getPosition(), leftFolded, node.getOperator(), rightFolded);
        }

        LiteralNode lLit = (LiteralNode) leftFolded;
        LiteralNode rLit = (LiteralNode) rightFolded;
        Object lv = lLit.getValue();
        Object rv = rLit.getValue();
        Position pos = node.getPosition();

        switch (node.getOperator()) {
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                if (lv instanceof Number && rv instanceof Number) {
                    return foldNumeric(node.getOperator(), pos, (Number) lv, (Number) rv);
                }
                break;
            case AND:
            case OR:
            case XOR:
                if (lv instanceof Boolean && rv instanceof Boolean) {
                    boolean lb = (Boolean) lv;
                    boolean rb = (Boolean) rv;
                    boolean res;
                    switch (node.getOperator()) {
                        case AND: res = lb && rb; break;
                        case OR:  res = lb || rb; break;
                        case XOR: res = lb ^ rb; break;
                        default:  res = false;   break;
                    }
                    return new LiteralNode(pos, res);
                }
                break;
            case LT:
            case LE:
            case GT:
            case GE:
            case EQ:
            case NE:
                // Comparisons
                if (lv instanceof Number && rv instanceof Number) {
                    double ld = ((Number) lv).doubleValue();
                    double rd = ((Number) rv).doubleValue();
                    boolean res;
                    switch (node.getOperator()) {
                        case LT: res = ld < rd;  break;
                        case LE: res = ld <= rd; break;
                        case GT: res = ld > rd;  break;
                        case GE: res = ld >= rd; break;
                        case EQ: res = ld == rd; break;
                        case NE: res = ld != rd; break;
                        default: res = false;    break;
                    }
                    return new LiteralNode(pos, res);
                }
                if (lv instanceof Boolean && rv instanceof Boolean &&
                    (node.getOperator() == BinaryExpressionNode.Operator.EQ ||
                     node.getOperator() == BinaryExpressionNode.Operator.NE)) {
                    boolean lb = (Boolean) lv;
                    boolean rb = (Boolean) rv;
                    boolean res = (node.getOperator() == BinaryExpressionNode.Operator.EQ) ? (lb == rb) : (lb != rb);
                    return new LiteralNode(pos, res);
                }
                break;
        }

        // Fallback: keep expression structure, but with folded children
        return new BinaryExpressionNode(pos, leftFolded, node.getOperator(), rightFolded);
    }

    private ASTNode foldNumeric(BinaryExpressionNode.Operator op, Position pos, Number ln, Number rn) {
        boolean isReal = (ln instanceof Double || ln instanceof Float || rn instanceof Double || rn instanceof Float);
        if (!isReal) {
            long a = ln.longValue();
            long b = rn.longValue();
            long res;
            switch (op) {
                case PLUS:     res = a + b; break;
                case MINUS:    res = a - b; break;
                case MULTIPLY: res = a * b; break;
                case DIVIDE:
                    if (b == 0) return new LiteralNode(pos, 0L); // avoid crash; runtime semantics will trap elsewhere
                    res = a / b; break;
                case MODULO:
                    if (b == 0) return new LiteralNode(pos, 0L);
                    res = a % b; break;
                default:
                    res = 0L; break;
            }
            return new LiteralNode(pos, Long.valueOf(res));
        } else {
            double a = ln.doubleValue();
            double b = rn.doubleValue();
            double res;
            switch (op) {
                case PLUS:     res = a + b; break;
                case MINUS:    res = a - b; break;
                case MULTIPLY: res = a * b; break;
                case DIVIDE:
                    res = a / b; break;
                case MODULO:
                    res = a % b; break;
                default:
                    res = 0.0; break;
            }
            return new LiteralNode(pos, Double.valueOf(res));
        }
    }
}
