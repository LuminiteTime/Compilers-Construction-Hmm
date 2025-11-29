package com.languagei.compiler.semantic;

import com.languagei.compiler.ast.*;
import java.util.*;

/**
 * Semantic analyzer - performs type checking and validation
 */
public class SemanticAnalyzer implements ASTVisitor {
    private final SymbolTable symbolTable;
    private final List<CompilationError> errors;
    private Type currentExpressionType;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
    }

    public void analyze(ProgramNode program) {
        // First pass: collect declarations
        for (ASTNode decl : program.getDeclarations()) {
            if (decl instanceof RoutineDeclarationNode) {
                RoutineDeclarationNode routine = (RoutineDeclarationNode) decl;
                List<Type> paramTypes = new ArrayList<>();
                for (ParameterNode param : routine.getParameters()) {
                    paramTypes.add(typeFromNode(param.getType()));
                }
                Type returnType = routine.getReturnType() != null ? 
                    typeFromNode(routine.getReturnType()) : Type.VOID;
                
                Type funcType = new Type.FunctionType(paramTypes, returnType);
                symbolTable.declare(routine.getName(), new Symbol(routine.getName(), Symbol.Kind.FUNCTION, funcType));
            } else if (decl instanceof TypeDeclarationNode) {
                TypeDeclarationNode typeDecl = (TypeDeclarationNode) decl;
                // Will handle type creation properly
            }
        }

        // Second pass: type check and validate
        for (ASTNode decl : program.getDeclarations()) {
            decl.accept(this);
        }
    }

    private Type typeFromNode(ASTNode node) {
        if (node instanceof PrimitiveTypeNode) {
            PrimitiveTypeNode prim = (PrimitiveTypeNode) node;
            return switch(prim.getType()) {
                case INTEGER -> Type.INTEGER;
                case REAL -> Type.REAL;
                case BOOLEAN -> Type.BOOLEAN;
            };
        } else if (node instanceof ArrayTypeNode) {
            ArrayTypeNode arr = (ArrayTypeNode) node;
            Type elemType = typeFromNode(arr.getElementType());
            Long size = null;
            if (arr.getSizeExpression() != null) {
                size = evaluateConstantExpression(arr.getSizeExpression());
            }
            return new Type.ArrayType(elemType, size);
        } else if (node instanceof RecordTypeNode) {
            RecordTypeNode rec = (RecordTypeNode) node;
            Type.RecordType recordType = new Type.RecordType("record");
            for (VariableDeclarationNode field : rec.getFields()) {
                Type fieldType = typeFromNode(field.getType());
                recordType.addField(field.getName(), fieldType);
            }
            return recordType;
        } else if (node instanceof TypeReferenceNode) {
            TypeReferenceNode ref = (TypeReferenceNode) node;
            Symbol sym = symbolTable.lookup(ref.getName());
            if (sym != null) {
                return sym.getType();
            }
            addError(node.getPosition(), "Unknown type: " + ref.getName());
            return Type.VOID;
        }
        return Type.VOID;
    }

    private Long evaluateConstantExpression(ASTNode expr) {
        if (expr instanceof LiteralNode) {
            Object val = ((LiteralNode) expr).getValue();
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        }
        return null;
    }

    @Override
    public void visit(ProgramNode node) {
        // First pass: collect declarations
        for (ASTNode decl : node.getDeclarations()) {
            decl.accept(this);
        }

        // Second pass: analyze statements
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(VariableDeclarationNode node) {
        Type type = null;
        
        if (node.getType() != null) {
            type = typeFromNode(node.getType());
        }
        
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
            Type initType = currentExpressionType;
            
            if (type == null) {
                type = initType;
            } else if (!isAssignmentCompatible(type, initType)) {
                addError(node.getPosition(), 
                    String.format("Type mismatch: cannot assign %s to %s", 
                        initType.getName(), type.getName()));
            }
        }
        
        if (type == null) {
            addError(node.getPosition(), "Cannot infer type for variable " + node.getName());
            type = Type.VOID;
        }
        
        if (symbolTable.isDeclaredInCurrentScope(node.getName())) {
            addError(node.getPosition(), "Duplicate declaration: " + node.getName());
        } else {
            symbolTable.declare(node.getName(), new Symbol(node.getName(), Symbol.Kind.VARIABLE, type));
        }
    }

    @Override
    public void visit(TypeDeclarationNode node) {
        Type type = typeFromNode(node.getType());
        symbolTable.declare(node.getName(), new Symbol(node.getName(), Symbol.Kind.TYPE, type));
    }

    @Override
    public void visit(RoutineDeclarationNode node) {
        symbolTable.enterScope();
        
        // Declare parameters
        for (ParameterNode param : node.getParameters()) {
            Type paramType = typeFromNode(param.getType());
            symbolTable.declare(param.getName(), new Symbol(param.getName(), Symbol.Kind.VARIABLE, paramType));
        }
        
        // Visit body
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        symbolTable.exitScope();
    }

    @Override
    public void visit(PrimitiveTypeNode node) {
        // Primitive types are handled elsewhere
    }

    @Override
    public void visit(ArrayTypeNode node) {
        // Array types are handled elsewhere
    }

    @Override
    public void visit(RecordTypeNode node) {
        for (VariableDeclarationNode field : node.getFields()) {
            field.accept(this);
        }
    }

    @Override
    public void visit(TypeReferenceNode node) {
        // Type references are handled elsewhere
    }

    @Override
    public void visit(BinaryExpressionNode node) {
        node.getLeft().accept(this);
        Type leftType = currentExpressionType;
        
        node.getRight().accept(this);
        Type rightType = currentExpressionType;
        
        currentExpressionType = getBinaryExpressionType(node.getOperator(), leftType, rightType, node.getPosition());
    }

    private Type getBinaryExpressionType(BinaryExpressionNode.Operator op, Type left, Type right, com.languagei.compiler.lexer.Position pos) {
        return switch(op) {
            case PLUS, MINUS, MULTIPLY, DIVIDE, MODULO -> {
                if ((left == Type.INTEGER || left == Type.REAL) && (right == Type.INTEGER || right == Type.REAL)) {
                    if (left == Type.REAL || right == Type.REAL) {
                        yield Type.REAL;
                    }
                    yield Type.INTEGER;
                } else {
                    addError(pos, "Invalid operand types for arithmetic operation");
                    yield Type.VOID;
                }
            }
            case AND, OR, XOR -> {
                if (left == Type.BOOLEAN && right == Type.BOOLEAN) {
                    yield Type.BOOLEAN;
                } else {
                    addError(pos, "Invalid operand types for logical operation");
                    yield Type.VOID;
                }
            }
            case LT, LE, GT, GE, EQ, NE -> Type.BOOLEAN;
        };
    }

    @Override
    public void visit(UnaryExpressionNode node) {
        node.getOperand().accept(this);
        Type operandType = currentExpressionType;
        
        currentExpressionType = switch(node.getOperator()) {
            case PLUS, MINUS -> {
                if (operandType == Type.INTEGER || operandType == Type.REAL) {
                    yield operandType;
                } else {
                    addError(node.getPosition(), "Invalid operand type for unary operator");
                    yield Type.VOID;
                }
            }
            case NOT -> {
                if (operandType == Type.BOOLEAN) {
                    yield Type.BOOLEAN;
                } else {
                    addError(node.getPosition(), "Invalid operand type for NOT operator");
                    yield Type.VOID;
                }
            }
        };
    }

    @Override
    public void visit(LiteralNode node) {
        Object value = node.getValue();
        if (value instanceof Long || value instanceof Integer) {
            currentExpressionType = Type.INTEGER;
        } else if (value instanceof Double || value instanceof Float) {
            currentExpressionType = Type.REAL;
        } else if (value instanceof Boolean) {
            currentExpressionType = Type.BOOLEAN;
        } else {
            currentExpressionType = Type.VOID;
        }
    }

    @Override
    public void visit(IdentifierNode node) {
        Symbol sym = symbolTable.lookup(node.getName());
        if (sym == null) {
            addError(node.getPosition(), "Undefined variable: " + node.getName());
            currentExpressionType = Type.VOID;
        } else {
            currentExpressionType = sym.getType();
        }
    }

    @Override
    public void visit(ArrayAccessNode node) {
        node.getArray().accept(this);
        Type arrayType = currentExpressionType;
        
        node.getIndex().accept(this);
        Type indexType = currentExpressionType;
        
        if (!(arrayType instanceof Type.ArrayType)) {
            addError(node.getPosition(), "Cannot index non-array type");
            currentExpressionType = Type.VOID;
        } else if (indexType != Type.INTEGER) {
            addError(node.getPosition(), "Array index must be integer");
            currentExpressionType = Type.VOID;
        } else {
            Type.ArrayType arr = (Type.ArrayType) arrayType;
            currentExpressionType = arr.getElementType();
        }
    }

    @Override
    public void visit(RecordAccessNode node) {
        node.getObject().accept(this);
        Type objectType = currentExpressionType;
        
        if ("length".equals(node.getFieldName()) || "size".equals(node.getFieldName())) {
            if (objectType instanceof Type.ArrayType) {
                currentExpressionType = Type.INTEGER;
            } else {
                addError(node.getPosition(), "Cannot access .size on non-array type");
                currentExpressionType = Type.VOID;
            }
        } else if (objectType instanceof Type.RecordType) {
            Type.RecordType record = (Type.RecordType) objectType;
            Type fieldType = record.getFieldType(node.getFieldName());
            if (fieldType != null) {
                currentExpressionType = fieldType;
            } else {
                addError(node.getPosition(), "Unknown field: " + node.getFieldName());
                currentExpressionType = Type.VOID;
            }
        } else {
            addError(node.getPosition(), "Cannot access field of non-record type");
            currentExpressionType = Type.VOID;
        }
    }

    @Override
    public void visit(RoutineCallNode node) {
        Symbol sym = symbolTable.lookup(node.getName());
        if (sym == null || !(sym.getType() instanceof Type.FunctionType)) {
            addError(node.getPosition(), "Undefined function: " + node.getName());
            currentExpressionType = Type.VOID;
        } else {
            Type.FunctionType funcType = (Type.FunctionType) sym.getType();
            
            List<Type> paramTypes = funcType.getParameterTypes();
            if (node.getArguments().size() != paramTypes.size()) {
                addError(node.getPosition(), 
                    String.format("Function %s expects %d arguments but got %d",
                        node.getName(), paramTypes.size(), node.getArguments().size()));
            }
            
            for (int i = 0; i < node.getArguments().size(); i++) {
                node.getArguments().get(i).accept(this);
                Type argType = currentExpressionType;
                if (i < paramTypes.size() && !isAssignmentCompatible(paramTypes.get(i), argType)) {
                    addError(node.getArguments().get(i).getPosition(), 
                        String.format("Argument type mismatch: expected %s but got %s",
                            paramTypes.get(i).getName(), argType.getName()));
                }
            }
            
            currentExpressionType = funcType.getReturnType();
        }
    }

    @Override
    public void visit(AssignmentNode node) {
        node.getTarget().accept(this);
        Type targetType = currentExpressionType;
        
        node.getValue().accept(this);
        Type valueType = currentExpressionType;
        
        if (!isAssignmentCompatible(targetType, valueType)) {
            addError(node.getPosition(),
                String.format("Type mismatch in assignment: cannot assign %s to %s",
                    valueType.getName(), targetType.getName()));
        }
    }

    @Override
    public void visit(IfStatementNode node) {
        node.getCondition().accept(this);
        if (currentExpressionType != Type.BOOLEAN) {
            addError(node.getPosition(), "If condition must be boolean");
        }
        
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }
    }

    @Override
    public void visit(WhileLoopNode node) {
        node.getCondition().accept(this);
        if (currentExpressionType != Type.BOOLEAN) {
            addError(node.getPosition(), "While condition must be boolean");
        }
        
        node.getBody().accept(this);
    }

    @Override
    public void visit(ForLoopNode node) {
        symbolTable.enterScope();
        
        // Loop variable is implicitly declared
        symbolTable.declare(node.getVariable(), new Symbol(node.getVariable(), Symbol.Kind.VARIABLE, Type.INTEGER));
        
        // Validate range expressions
        if (node.getRangeStart() != null && node.getRangeEnd() != null) {
            node.getRangeStart().accept(this);
            if (currentExpressionType != Type.INTEGER) {
                addError(node.getPosition(), "Range start must be integer");
            }
            
            node.getRangeEnd().accept(this);
            if (currentExpressionType != Type.INTEGER) {
                addError(node.getPosition(), "Range end must be integer");
            }
        } else if (node.getArrayExpr() != null) {
            node.getArrayExpr().accept(this);
            if (!(currentExpressionType instanceof Type.ArrayType)) {
                addError(node.getPosition(), "For loop array expression must be array type");
            }
        }
        
        node.getBody().accept(this);
        
        symbolTable.exitScope();
    }

    @Override
    public void visit(ReturnStatementNode node) {
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
    }

    @Override
    public void visit(PrintStatementNode node) {
        for (ASTNode expr : node.getExpressions()) {
            expr.accept(this);
        }
    }

    @Override
    public void visit(BlockNode node) {
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
    }

    private boolean isAssignmentCompatible(Type target, Type source) {
        if (target.equals(source)) {
            return true;
        }
        
        // Integer can receive from integer
        if (target == Type.INTEGER && source == Type.INTEGER) return true;
        
        // Integer can receive from real (with conversion)
        if (target == Type.INTEGER && source == Type.REAL) return true;
        
        // Integer can receive from boolean
        if (target == Type.INTEGER && source == Type.BOOLEAN) return true;
        
        // Real can receive from real
        if (target == Type.REAL && source == Type.REAL) return true;
        
        // Real can receive from integer
        if (target == Type.REAL && source == Type.INTEGER) return true;
        
        // Real can receive from boolean
        if (target == Type.REAL && source == Type.BOOLEAN) return true;
        
        // Boolean can receive from boolean
        if (target == Type.BOOLEAN && source == Type.BOOLEAN) return true;
        
        // Boolean can receive from integer (only 0 or 1)
        if (target == Type.BOOLEAN && source == Type.INTEGER) return true;
        
        return false;
    }

    private void addError(com.languagei.compiler.lexer.Position position, String message) {
        errors.add(new CompilationError(CompilationError.Severity.ERROR, message, position));
    }

    public List<CompilationError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

