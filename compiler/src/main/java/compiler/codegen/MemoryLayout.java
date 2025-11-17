package compiler.codegen;

import java.util.*;

/**
 * Manages memory layout for arrays and records
 */
public class MemoryLayout {
    private static final int HEAP_START = 0x1000;  // 4KB reserved space
    private int heapPointer = HEAP_START;
    private Map<String, RecordLayout> recordLayouts = new HashMap<>();

    public class RecordLayout {
        public String recordName;
        public int totalSize;
        public Map<String, Integer> fieldOffsets = new LinkedHashMap<>();

        public RecordLayout(String name) {
            this.recordName = name;
            this.totalSize = 0;
        }

        public void addField(String fieldName, String fieldType) {
            int size = getTypeSize(fieldType);
            int offset = alignOffset(totalSize, size);
            fieldOffsets.put(fieldName, offset);
            totalSize = offset + size;
        }

        public int getFieldOffset(String fieldName) {
            Integer offset = fieldOffsets.get(fieldName);
            if (offset == null) {
                throw new CodeGenException("Unknown record field: " + fieldName);
            }
            return offset;
        }

        public int getTotalSize() {
            return totalSize;
        }
    }

    /**
     * Register a record type with its fields
     */
    public RecordLayout registerRecord(String name) {
        RecordLayout layout = new RecordLayout(name);
        recordLayouts.put(name, layout);
        return layout;
    }

    /**
     * Get record layout
     */
    public RecordLayout getRecordLayout(String name) {
        RecordLayout layout = recordLayouts.get(name);
        if (layout == null) {
            throw new CodeGenException("Unknown record type: " + name);
        }
        return layout;
    }

    /**
     * Allocate memory for array
     * Returns pointer to allocated memory
     */
    public int allocateArray(int elementCount, String elementType) {
        int elementSize = getTypeSize(elementType);
        int totalSize = (elementCount + 1) * 4;  // +1 for size field, each element 4 bytes (or 8 for real)
        
        if (elementType.equalsIgnoreCase("real")) {
            totalSize = 4 + (elementCount * 8);  // size field (4) + elements (8 each)
        }

        int ptr = heapPointer;
        heapPointer += totalSize;
        return ptr;
    }

    /**
     * Allocate memory for record
     */
    public int allocateRecord(String recordName) {
        RecordLayout layout = getRecordLayout(recordName);
        int ptr = heapPointer;
        heapPointer += layout.getTotalSize();
        return ptr;
    }

    /**
     * Calculate aligned offset
     */
    public int alignOffset(int currentOffset, int typeSize) {
        int alignment = getAlignment(typeSize);
        return (currentOffset + alignment - 1) & ~(alignment - 1);
    }

    /**
     * Get type size in bytes
     */
    public static int getTypeSize(String type) {
        return switch (type.toLowerCase()) {
            case "integer", "boolean" -> 4;
            case "real" -> 8;
            case "array", "record" -> 4;  // pointer
            default -> throw new CodeGenException("Unknown type: " + type);
        };
    }

    /**
     * Get type alignment requirement
     */
    public static int getAlignment(int typeSize) {
        if (typeSize >= 8) return 8;
        if (typeSize >= 4) return 4;
        if (typeSize >= 2) return 2;
        return 1;
    }

    /**
     * Get current heap pointer
     */
    public int getHeapPointer() {
        return heapPointer;
    }

    /**
     * Calculate array element access address
     * Address = base + (index-1)*element_size + 4 (skip size field)
     */
    public String calculateArrayElementAddress(String baseReg, String indexReg, int elementSize) {
        return String.format(
            "local.get %s\n" +
            "local.get %s\n" +
            "i32.const 1\n" +
            "i32.sub\n" +
            "i32.const %d\n" +
            "i32.mul\n" +
            "i32.add\n" +
            "i32.const 4\n" +
            "i32.add",
            baseReg, indexReg, elementSize
        );
    }

    /**
     * Get load instruction for type
     */
    public static String getLoadInstruction(String type) {
        return switch (type.toLowerCase()) {
            case "integer", "boolean", "array", "record" -> "i32.load";
            case "real" -> "f64.load";
            default -> throw new CodeGenException("Unknown type: " + type);
        };
    }

    /**
     * Get store instruction for type
     */
    public static String getStoreInstruction(String type) {
        return switch (type.toLowerCase()) {
            case "integer", "boolean", "array", "record" -> "i32.store";
            case "real" -> "f64.store";
            default -> throw new CodeGenException("Unknown type: " + type);
        };
    }
}

