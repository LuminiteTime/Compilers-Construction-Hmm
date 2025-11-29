package com.languagei.compiler.semantic;

import java.util.*;

/**
 * Represents a type in Language I
 */
public abstract class Type {
    public static final IntegerType INTEGER = new IntegerType();
    public static final RealType REAL = new RealType();
    public static final BooleanType BOOLEAN = new BooleanType();
    public static final VoidType VOID = new VoidType();

    public abstract String getName();
    public abstract boolean isCompatibleWith(Type other);

    public static class IntegerType extends Type {
        @Override
        public String getName() { return "integer"; }
        
        @Override
        public boolean isCompatibleWith(Type other) {
            return other instanceof IntegerType;
        }
        
        @Override
        public boolean equals(Object o) { return o instanceof IntegerType; }
        
        @Override
        public int hashCode() { return "integer".hashCode(); }
    }

    public static class RealType extends Type {
        @Override
        public String getName() { return "real"; }
        
        @Override
        public boolean isCompatibleWith(Type other) {
            return other instanceof RealType;
        }
        
        @Override
        public boolean equals(Object o) { return o instanceof RealType; }
        
        @Override
        public int hashCode() { return "real".hashCode(); }
    }

    public static class BooleanType extends Type {
        @Override
        public String getName() { return "boolean"; }
        
        @Override
        public boolean isCompatibleWith(Type other) {
            return other instanceof BooleanType;
        }
        
        @Override
        public boolean equals(Object o) { return o instanceof BooleanType; }
        
        @Override
        public int hashCode() { return "boolean".hashCode(); }
    }

    public static class VoidType extends Type {
        @Override
        public String getName() { return "void"; }
        
        @Override
        public boolean isCompatibleWith(Type other) {
            return other instanceof VoidType;
        }
        
        @Override
        public boolean equals(Object o) { return o instanceof VoidType; }
        
        @Override
        public int hashCode() { return "void".hashCode(); }
    }

    public static class ArrayType extends Type {
        private final Type elementType;
        private final Long size; // null for unbounded arrays
        private final String name;

        public ArrayType(Type elementType, Long size) {
            this.elementType = elementType;
            this.size = size;
            this.name = "array of " + elementType.getName();
        }

        public Type getElementType() { return elementType; }
        public Long getSize() { return size; }

        @Override
        public String getName() { return name; }

        @Override
        public boolean isCompatibleWith(Type other) {
            if (!(other instanceof ArrayType)) return false;
            ArrayType that = (ArrayType) other;
            return this.elementType.equals(that.elementType) &&
                   Objects.equals(this.size, that.size);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrayType)) return false;
            ArrayType that = (ArrayType) o;
            return this.elementType.equals(that.elementType) &&
                   Objects.equals(this.size, that.size);
        }

        @Override
        public int hashCode() { return Objects.hash(elementType, size); }
    }

    public static class RecordType extends Type {
        private final String name;
        private final Map<String, Type> fields;

        public RecordType(String name) {
            this.name = name;
            this.fields = new LinkedHashMap<>();
        }

        public void addField(String fieldName, Type type) {
            fields.put(fieldName, type);
        }

        public Type getFieldType(String fieldName) {
            return fields.get(fieldName);
        }

        public Map<String, Type> getFields() { return fields; }

        @Override
        public String getName() { return name; }

        @Override
        public boolean isCompatibleWith(Type other) {
            if (!(other instanceof RecordType)) return false;
            return this.name.equals(((RecordType) other).name);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RecordType)) return false;
            return this.name.equals(((RecordType) o).name);
        }

        @Override
        public int hashCode() { return name.hashCode(); }
    }

    public static class FunctionType extends Type {
        private final List<Type> paramTypes;
        private final Type returnType;

        public FunctionType(List<Type> paramTypes, Type returnType) {
            this.paramTypes = paramTypes;
            this.returnType = returnType;
        }

        public List<Type> getParameterTypes() { return paramTypes; }
        public Type getReturnType() { return returnType; }

        @Override
        public String getName() {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < paramTypes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(paramTypes.get(i).getName());
            }
            sb.append(") -> ").append(returnType.getName());
            return sb.toString();
        }

        @Override
        public boolean isCompatibleWith(Type other) {
            if (!(other instanceof FunctionType)) return false;
            FunctionType that = (FunctionType) other;
            return this.paramTypes.equals(that.paramTypes) && 
                   this.returnType.equals(that.returnType);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FunctionType)) return false;
            FunctionType that = (FunctionType) o;
            return this.paramTypes.equals(that.paramTypes) && 
                   this.returnType.equals(that.returnType);
        }

        @Override
        public int hashCode() { return Objects.hash(paramTypes, returnType); }
    }
}

