package com.languagei.compiler.semantic;

import com.languagei.compiler.ast.*;

/**
 * Performs simple dead code elimination based on constant boolean conditions.
 */
public class DeadCodeEliminator {

    public ProgramNode optimize(ProgramNode root) {
        return (ProgramNode) eliminate(root);
    }

    private ASTNode eliminate(ASTNode node) {
        if (node == null) return null;

        if (node instanceof ProgramNode) {
            ProgramNode prog = (ProgramNode) node;
            ProgramNode result = new ProgramNode(prog.getPosition());
            for (ASTNode decl : prog.getDeclarations()) {
                result.addDeclaration(eliminate(decl));
            }
            for (ASTNode stmt : prog.getStatements()) {
                appendStatement(result.getStatements(), eliminateStatement(stmt));
            }
            return result;
        }

        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            BlockNode result = new BlockNode(block.getPosition());
            for (ASTNode stmt : block.getStatements()) {
                appendStatement(result.getStatements(), eliminateStatement(stmt));
            }
            return result;
        }

        // For all other nodes, just recurse into children where needed
        if (node instanceof IfStatementNode) {
            IfStatementNode i = (IfStatementNode) node;
            ASTNode cond = eliminate(i.getCondition());
            BlockNode thenBlock = (BlockNode) eliminate(i.getThenBlock());
            BlockNode elseBlock = i.getElseBlock() != null ? (BlockNode) eliminate(i.getElseBlock()) : null;
            return new IfStatementNode(i.getPosition(), cond, thenBlock, elseBlock);
        }

        if (node instanceof WhileLoopNode) {
            WhileLoopNode w = (WhileLoopNode) node;
            ASTNode cond = eliminate(w.getCondition());
            BlockNode body = (BlockNode) eliminate(w.getBody());
            return new WhileLoopNode(w.getPosition(), cond, body);
        }

        if (node instanceof VariableDeclarationNode) {
            VariableDeclarationNode v = (VariableDeclarationNode) node;
            ASTNode type = v.getType();
            ASTNode init = v.getInitializer() != null ? eliminate(v.getInitializer()) : null;
            return new VariableDeclarationNode(v.getPosition(), v.getName(), type, init);
        }

        if (node instanceof AssignmentNode) {
            AssignmentNode a = (AssignmentNode) node;
            return new AssignmentNode(a.getPosition(), eliminate(a.getTarget()), eliminate(a.getValue()));
        }

        if (node instanceof PrintStatementNode) {
            PrintStatementNode p = (PrintStatementNode) node;
            PrintStatementNode res = new PrintStatementNode(p.getPosition());
            for (ASTNode e : p.getExpressions()) {
                res.addExpression(eliminate(e));
            }
            return res;
        }

        if (node instanceof ReturnStatementNode) {
            ReturnStatementNode r = (ReturnStatementNode) node;
            ASTNode value = r.getValue() != null ? eliminate(r.getValue()) : null;
            return new ReturnStatementNode(r.getPosition(), value);
        }

        if (node instanceof RoutineDeclarationNode) {
            RoutineDeclarationNode r = (RoutineDeclarationNode) node;
            BlockNode body = r.getBody() != null ? (BlockNode) eliminate(r.getBody()) : null;
            return new RoutineDeclarationNode(r.getPosition(), r.getName(), r.getParameters(), r.getReturnType(), body);
        }

        if (node instanceof RoutineCallNode) {
            RoutineCallNode call = (RoutineCallNode) node;
            RoutineCallNode res = new RoutineCallNode(call.getPosition(), call.getName());
            for (ASTNode arg : call.getArguments()) {
                res.addArgument(eliminate(arg));
            }
            return res;
        }

        if (node instanceof ArrayAccessNode) {
            ArrayAccessNode a = (ArrayAccessNode) node;
            return new ArrayAccessNode(a.getPosition(), eliminate(a.getArray()), eliminate(a.getIndex()));
        }

        if (node instanceof RecordAccessNode) {
            RecordAccessNode r = (RecordAccessNode) node;
            return new RecordAccessNode(r.getPosition(), eliminate(r.getObject()), r.getFieldName());
        }

        // Literals, identifiers, types, etc. stay as-is
        return node;
    }

    /**
     * Process a statement for elimination. Returns either the same statement,
     * a transformed one, an array of statements (flattened), or null (removed).
     */
    private ASTNode eliminateStatement(ASTNode stmt) {
        if (stmt instanceof IfStatementNode) {
            IfStatementNode i = (IfStatementNode) eliminate(stmt);
            if (isFalseLiteral(i.getCondition())) {
                // if false then ... else ... end
                if (i.getElseBlock() != null) {
                    // Replace whole if by else-block contents
                    BlockNode elseBlock = i.getElseBlock();
                    BlockNode flattened = new BlockNode(elseBlock.getPosition());
                    for (ASTNode s : elseBlock.getStatements()) {
                        appendStatement(flattened.getStatements(), eliminateStatement(s));
                    }
                    return flattened;
                } else {
                    // if false then ... end  -> remove entirely
                    return null;
                }
            }
            return i;
        }

        if (stmt instanceof WhileLoopNode) {
            WhileLoopNode w = (WhileLoopNode) eliminate(stmt);
            if (isFalseLiteral(w.getCondition())) {
                // while false loop ... end -> remove entirely
                return null;
            }
            return w;
        }

        return eliminate(stmt);
    }

    private boolean isFalseLiteral(ASTNode cond) {
        if (cond instanceof LiteralNode) {
            Object v = ((LiteralNode) cond).getValue();
            return Boolean.FALSE.equals(v);
        }
        return false;
    }

    private void appendStatement(java.util.List<ASTNode> list, ASTNode stmt) {
        if (stmt == null) return;
        if (stmt instanceof BlockNode) {
            // Flatten embedded block that came from eliminating an if
            BlockNode block = (BlockNode) stmt;
            for (ASTNode s : block.getStatements()) {
                appendStatement(list, s);
            }
        } else {
            list.add(stmt);
        }
    }
}
