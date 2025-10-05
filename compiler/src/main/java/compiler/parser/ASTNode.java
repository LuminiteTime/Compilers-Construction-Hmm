package compiler.parser;

import java.util.List;

/**
 * Base interface for all AST nodes in the Imperative (I) language compiler.
 */
interface ASTNode {
    /**
     * Returns a string representation of the AST node for debugging and tree printing.
     */
    String toString();
}


/**
 * Abstract base class for all declaration nodes.
 */
abstract class Declaration implements ASTNode {
}

/**
 * Represents a variable declaration (var name : type [is expression] | var name is expression).
 */
class VariableDeclaration extends Declaration {
    public final String name;
    public final Type type; // null if inferred from initializer
    public final Expression initializer; // null if not specified

    public VariableDeclaration(String name, Type type, Expression initializer) {
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VariableDeclaration: ").append(name);
        if (type != null) {
            sb.append(" : ").append(type.toString());
        }
        if (initializer != null) {
            sb.append(" is ").append(initializer.toString());
        }
        return sb.toString();
    }
}

/**
 * Represents a type declaration (type name is type).
 */
class TypeDeclaration extends Declaration {
    public final String name;
    public final Type type;

    public TypeDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "TypeDeclaration: " + name + " is " + type.toString();
    }
}

/**
 * Represents a routine declaration (routine name(parameters) [: returnType] [is body | => expression]).
 */
class RoutineDeclaration extends Declaration {
    public final String name;
    public final List<Parameter> parameters;
    public final Type returnType; // null for procedures
    public final RoutineBody body; // null for forward declarations

    public RoutineDeclaration(String name, List<Parameter> parameters, Type returnType, RoutineBody body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RoutineDeclaration: ").append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).toString());
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(" : ").append(returnType.toString());
        }
        if (body != null) {
            sb.append(" ").append(body.toString());
        }
        return sb.toString();
    }
}

/**
 * Represents a parameter in a routine declaration (name : type).
 */
class Parameter {
    public final String name;
    public final Type type;

    public Parameter(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + " : " + type.toString();
    }
}

/**
 * Abstract base class for all type nodes.
 */
abstract class Type implements ASTNode {
}

/**
 * Represents a primitive type (integer, real, boolean).
 */
class PrimitiveType extends Type {
    public final String typeName; // "integer", "real", or "boolean"

    public PrimitiveType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}

/**
 * Represents an array type (array[size] elementType).
 */
class ArrayType extends Type {
    public final Expression size; // null for sizeless parameters
    public final Type elementType;

    public ArrayType(Expression size, Type elementType) {
        this.size = size;
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("array");
        if (size != null) {
            sb.append("[").append(size.toString()).append("]");
        }
        sb.append(" ").append(elementType.toString());
        return sb.toString();
    }
}

/**
 * Represents a record type (record { fieldDeclarations } end).
 */
class RecordType extends Type {
    public final List<VariableDeclaration> fields;

    public RecordType(List<VariableDeclaration> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("record\n");
        for (VariableDeclaration field : fields) {
            sb.append("  ").append(field.toString().replace("\n", "\n  ")).append("\n");
        }
        sb.append("end");
        return sb.toString();
    }
}

/**
 * Represents a named/user-defined type.
 */
class NamedType extends Type {
    public final String typeName;

    public NamedType(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}

/**
 * Abstract base class for routine body nodes.
 */
abstract class RoutineBody implements ASTNode {
}

/**
 * Represents a full routine body with declarations and statements (is body end).
 */
class FullBody extends RoutineBody {
    public final List<ASTNode> declarationsAndStatements; // Declaration or Statement

    public FullBody(List<ASTNode> declarationsAndStatements) {
        this.declarationsAndStatements = declarationsAndStatements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("is\n");
        for (ASTNode node : declarationsAndStatements) {
            sb.append("  ").append(node.toString().replace("\n", "\n  ")).append("\n");
        }
        sb.append("end");
        return sb.toString();
    }
}

/**
 * Represents an expression body (=> expression).
 */
class ExpressionBody extends RoutineBody {
    public final Expression expression;

    public ExpressionBody(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "=> " + expression.toString();
    }
}

/**
 * Abstract base class for all statement nodes.
 */
abstract class Statement implements ASTNode {
}

/**
 * Represents an assignment statement (modifiablePrimary := expression).
 */
class AssignmentStatement extends Statement {
    public final ModifiablePrimary target;
    public final Expression value;

    public AssignmentStatement(ModifiablePrimary target, Expression value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Assignment: " + target.toString() + " := " + value.toString();
    }
}

/**
 * Represents a routine call statement (identifier[(expression {, expression})]).
 */
class RoutineCallStatement extends Statement {
    public final String routineName;
    public final List<Expression> arguments;

    public RoutineCallStatement(String routineName, List<Expression> arguments) {
        this.routineName = routineName;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Call: ").append(routineName);
        if (!arguments.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toString());
            }
            sb.append(")");
        }
        return sb.toString();
    }
}

/**
 * Represents a while loop statement (while expression loop body end).
 */
class WhileLoopStatement extends Statement {
    public final Expression condition;
    public final List<ASTNode> body; // Declaration or Statement

    public WhileLoopStatement(Expression condition, List<ASTNode> body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("While: ").append(condition.toString()).append("\n");
        sb.append("  loop\n");
        for (ASTNode node : body) {
            sb.append("    ").append(node.toString().replace("\n", "\n    ")).append("\n");
        }
        sb.append("  end");
        return sb.toString();
    }
}

/**
 * Represents a for loop statement (for identifier in range [reverse] loop body end).
 */
class ForLoopStatement extends Statement {
    public final String loopVariable;
    public final Range range;
    public final boolean reverse;
    public final List<ASTNode> body; // Declaration or Statement

    public ForLoopStatement(String loopVariable, Range range, boolean reverse, List<ASTNode> body) {
        this.loopVariable = loopVariable;
        this.range = range;
        this.reverse = reverse;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("For: ").append(loopVariable).append(" in ").append(range.toString());
        if (reverse) {
            sb.append(" reverse");
        }
        sb.append("\n  loop\n");
        for (ASTNode node : body) {
            sb.append("    ").append(node.toString().replace("\n", "\n    ")).append("\n");
        }
        sb.append("  end");
        return sb.toString();
    }
}

/**
 * Represents an if statement (if expression then body [else body] end).
 */
class IfStatement extends Statement {
    public final Expression condition;
    public final List<ASTNode> thenBody;
    public final List<ASTNode> elseBody; // null if no else

    public IfStatement(Expression condition, List<ASTNode> thenBody, List<ASTNode> elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("If: ").append(condition.toString()).append("\n");
        sb.append("  then\n");
        for (ASTNode node : thenBody) {
            sb.append("    ").append(node.toString().replace("\n", "\n    ")).append("\n");
        }
        if (elseBody != null) {
            sb.append("  else\n");
            for (ASTNode node : elseBody) {
                sb.append("    ").append(node.toString().replace("\n", "\n    ")).append("\n");
            }
        }
        sb.append("  end");
        return sb.toString();
    }
}

/**
 * Represents a print statement (print expression {, expression}).
 */
class PrintStatement extends Statement {
    public final List<Expression> expressions;

    public PrintStatement(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Print:");
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(" ").append(expressions.get(i).toString());
        }
        return sb.toString();
    }
}

/**
 * Represents a return statement (return expression).
 */
class ReturnStatement extends Statement {
    public final Expression expression;

    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "Return: " + expression.toString();
    }
}

/**
 * Represents a range in for loops (expression [.. expression]).
 */
class Range {
    public final Expression start;
    public final Expression end; // null for array iteration

    public Range(Expression start, Expression end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        if (end == null) {
            return start.toString();
        }
        return start.toString() + ".." + end.toString();
    }
}

/**
 * Abstract base class for all expression nodes.
 */
abstract class Expression implements ASTNode {
}

/**
 * Represents a binary expression (left operator right).
 */
class BinaryExpression extends Expression {
    public final Expression left;
    public final Expression right;
    public final String operator;

    public BinaryExpression(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " " + operator + " " + right.toString() + ")";
    }
}

/**
 * Represents a unary expression (operator operand).
 */
class UnaryExpression extends Expression {
    public final Expression operand;
    public final String operator;

    public UnaryExpression(String operator, Expression operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public String toString() {
        return operator + operand.toString();
    }
}

/**
 * Represents a literal expression (integer, real, boolean, string).
 */
class LiteralExpression extends Expression {
    public final String value; // String representation of literal
    public final String type; // "integer", "real", "boolean", "string"

    public LiteralExpression(String value, String type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public String toString() {
        return value;
    }
}

/**
 * Represents a variable expression (identifier).
 */
class VariableExpression extends Expression {
    public final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * Represents a field access expression (record.fieldName).
 */
class FieldAccessExpression extends Expression {
    public final Expression record;
    public final String fieldName;

    public FieldAccessExpression(Expression record, String fieldName) {
        this.record = record;
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return record.toString() + "." + fieldName;
    }
}

/**
 * Represents an array access expression (array[index]).
 */
class ArrayAccessExpression extends Expression {
    public final Expression array;
    public final Expression index;

    public ArrayAccessExpression(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public String toString() {
        return array.toString() + "[" + index.toString() + "]";
    }
}

/**
 * Represents a routine call expression (identifier[(expression {, expression})]).
 */
class RoutineCallExpression extends Expression {
    public final String routineName;
    public final List<Expression> arguments;

    public RoutineCallExpression(String routineName, List<Expression> arguments) {
        this.routineName = routineName;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(routineName);
        if (!arguments.isEmpty()) {
            sb.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toString());
            }
            sb.append(")");
        }
        return sb.toString();
    }
}

/**
 * Represents a modifiable primary (identifier{.identifier | [expression]}*).
 */
class ModifiablePrimary implements ASTNode {
    public final String baseName;
    public final List<Access> accesses; // Chain of .field or [index]

    public ModifiablePrimary(String baseName, List<Access> accesses) {
        this.baseName = baseName;
        this.accesses = accesses;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(baseName);
        for (Access access : accesses) {
            if (access.isField) {
                sb.append(".").append(access.fieldName);
            } else {
                sb.append("[").append(access.index.toString()).append("]");
            }
        }
        return sb.toString();
    }
}

/**
 * Represents an access in a modifiable primary (either field access or array access).
 */
class Access {
    public final boolean isField; // true for .field, false for [index]
    public final String fieldName; // for field access
    public final Expression index; // for array access

    public Access(String fieldName) {
        this.isField = true;
        this.fieldName = fieldName;
        this.index = null;
    }

    public Access(Expression index) {
        this.isField = false;
        this.fieldName = null;
        this.index = index;
    }

    @Override
    public String toString() {
        if (isField) {
            return "." + fieldName;
        } else {
            return "[" + index.toString() + "]";
        }
    }
}
