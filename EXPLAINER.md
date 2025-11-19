## **ПОЛНОЕ ДЕТАЛЬНОЕ ОПИСАНИЕ ВСЕХ ФАЙЛОВ ПРОЕКТА КОМПИЛЯТОРА**

### **🔧 ОСНОВНЫЕ КОМПОНЕНТЫ КОМПИЛЯТОРА** 

ТЕСТИРОВАНИЕ - **./test_compiler.sh test**

##  **КОРНЕВЫЕ ФАЙЛЫ ПРОЕКТА**

### **`README.md`** - Основная документация проекта

```
Назначение: Полное описание проекта компилятора
Содержание:
- Информация о команде (Mikhail Trifonov, Kirill Efimovich)
- Архитектура: Java лексер + C++ парсер → WASM кодогенератор
- Примеры кода на языке I
- Инструкции по сборке и запуску
- Спецификация поддерживаемых возможностей языка
```

### **`QUICKSTART.md`** - Быстрый старт

```
Назначение: Краткое руководство по запуску
Содержание:
- Команды сборки (Gradle + Make)
- Тестирование в Docker
- Поддерживаемые возможности языка
```

### **`build.gradle`** - Корневая конфигурация Gradle

```gradle
// Root build file for multi-project build
allprojects {
    apply plugin: 'java'
    repositories {
        mavenCentral()
    }
    tasks.withType(JavaCompile) {
        options.encoding = 'UTF-8'
    }
}
```

### **`settings.gradle`** - Настройки Gradle

```gradle
rootProject.name = 'Compilers-Construction-Hmm'
include 'compiler'
include 'tests'
```

### **`gradle.properties`** - Свойства Gradle

```
# Gradle properties
# org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
org.gradle.jvmargs=-Xmx1024m
```

---

## **☕ JAVA КОМПОНЕНТЫ**

### **Основной компилятор**

#### **`Compiler.java`** - Точка входа компилятора

```java
package compiler;

public class Compiler {
    public static void main(String[] args) {
        // Загрузка JNI библиотеки
        // Чтение исходного файла
        // Токенизация через Java лексер
        // Парсинг через C++ компонент
        // Кодогенерация в WebAssembly
    }
}
```

**Ключевые функции:**

- Загрузка нативной JNI библиотеки (`libparser.so`)
- Чтение исходного файла из аргументов командной строки
- Инициализация Java лексера для токенизации
- Вызов C++ парсера через JNI
- Управление кодогенерацией в WebAssembly
- Обработка ошибок компиляции
- Запись результата в выходной файл

#### **`EndToEndTest.java`** - Интеграционные тесты

```java
package compiler;

public class EndToEndTest {
    public static void main(String[] args) {
        // Автоматическая загрузка JNI библиотеки
        // Тестирование различных категорий файлов
        // Сравнение ожидаемых результатов
        // Генерация отчетов
    }
}
```

**Функции:**

- Автоматическая загрузка JNI библиотеки
- Тестирование полного цикла компиляции
- Запуск тестов на различных типах файлов
- Сравнение ожидаемых и полученных результатов
- Генерация отчетов о прохождении тестов

---

### **Лексер (`lexer/`)`

#### **`Lexer.java`** - Основной класс лексера

```java
package compiler.lexer;

public class Lexer {
    private final PushbackReader reader;
    private int currentChar;
    private int line = 1;
    private int column = 1;
  
    // Ключевые слова
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
  
    static {
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("integer", TokenType.INTEGER);
        // ... остальные ключевые слова
    }
  
    public Token nextToken() throws LexerException {
        // Реализация конечного автомата лексера
    }
}
```

**Особенности реализации:**

- Ручная реализация конечного автомата (не Flex)
- Поддержка Unicode символов
- Точное отслеживание позиций (строка/столбец)
- Обработка комментариев (// и /* */)
- Строковые литералы с экранированием
- Числа с плавающей точкой и знаками
- JNI интеграция для передачи токенов в C++ парсер

#### **`Token.java`** - Структура токена

```java
package compiler.lexer;

public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;
    private final int endLine;
    private final int endColumn;
  
    public Token(TokenType type, String lexeme, int line, int column, int endLine, int endColumn) {
        // Конструктор токена
    }
  
    public TokenType getType() { return type; }
    public String toString() { return String.format("%s:%s@%d:%d", type, lexeme, line, column); }
}
```

#### **`TokenType.java`** - Перечисление типов токенов

```java
package compiler.lexer;

public enum TokenType {
    // Ключевые слова
    VAR, TYPE, IS, INTEGER, REAL, BOOLEAN, ARRAY, RECORD, END,
    WHILE, LOOP, FOR, IN, REVERSE, IF, THEN, ELSE, PRINT, ROUTINE,
    TRUE, FALSE, AND, OR, XOR, NOT, RETURN,
  
    // Литералы и идентификаторы
    IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL, STRING_LITERAL,
  
    // Операторы
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EQUAL, NOT_EQUAL,
    ASSIGN, RANGE,
  
    // Разделители
    COLON, SEMICOLON, COMMA, DOT, LPAREN, RPAREN, LBRACKET, RBRACKET, ARROW,
  
    // Конец файла
    EOF
}
```

#### **`LexerException.java`** - Исключения лексера

```java
package compiler.lexer;

public class LexerException extends Exception {
    public LexerException(String message, int line, int column, Throwable cause) {
        super(String.format("%s at line %d, column %d", message, line, column), cause);
    }
}
```

#### **`README.md`** - Документация лексера

```
Содержание: 
- Подробное описание работы лексера
- Примеры токенов для каждого тестового случая
- Архитектура конечного автомата
- Список всех поддерживаемых токенов
```

---

### **Кодогенератор (`codegen/`)`

#### **`WasmCodeGenerator.java`** - Основной генератор WASM

```java
package compiler.codegen;

public class WasmCodeGenerator {
    private CodeGenSymbolTable symbolTable;
    private StringBuilder watOutput;
    private int blockDepth = 0;
    private int loopDepth = 0;
    private int indentLevel = 0;

    // Глобальные ресурсы
    private int memoryPages = 1;
    private boolean hasMemory = false;
    private boolean hasHeapPtr = false;
    private List<String> functions = new ArrayList<>();
    private List<String> globals = new ArrayList<>();
    private List<String> imports = new ArrayList<>();
  
    public String generate(Object programAst) throws CodeGenException {
        // Генерация WebAssembly модуля
    }
}
```

**Функции:**

- Управление модулем WASM (импорты, память, глобальные переменные)
- Генерация функций печати (print_int, print_char)
- Управление памятью (куча, аллокатор)
- Генерация инструкций WASM для различных конструкций языка I
- Преобразование типов (integer ↔ real, boolean)

#### **`CppASTBridge.java`** - Мост к C++ AST

```java
package compiler.codegen;

public class CppASTBridge {
    private long astPointer;  // Указатель на C++ AST
    private WasmCodeGenerator generator;

    // JNI методы
    public native long getASTPointer();
    public native String generateWasmFromAST(long astPointer);
    public native String getASTAsJson(long astPointer);
  
    public String generate() {
        // Получение AST от C++ и генерация WASM
    }
}
```

**Функции:**

- Получение указателя на AST от C++ парсера
- Преобразование JSON представления AST в WASM код
- Простой JSON парсер для обработки AST
- Обработка различных типов узлов AST
- Генерация минимального модуля при ошибках

#### **`CodeGenSymbolTable.java`** - Таблица символов кодогенератора

```java
package compiler.codegen;

public class CodeGenSymbolTable {
    private final Stack<Map<String, SymbolInfo>> scopes;
    private final Map<String, SymbolInfo> globalFunctions;
    private int nextLocalIndex;
    private int nextGlobalIndex;
    private int nextFunctionIndex;
    private int heapPointer = 0x1000;
  
    public void declareLocal(String name, String type) {
        // Объявление локальной переменной
    }
  
    public SymbolInfo lookup(String name) {
        // Поиск символа в областях видимости
    }
}
```

#### **`CodeGenVisitor.java`** - Посетитель AST для кодогенерации

```java
package compiler.codegen;

public class CodeGenVisitor implements ASTVisitor {
    private WasmCodeGenerator generator;
    private String currentFunctionName;
    private String currentFunctionReturnType;

    @Override
    public void visitProgram(Object programNode) {
        // Обработка программы
    }
  
    @Override
    public void visitVariableDeclaration(Object node) {
        // Обработка объявления переменной
    }
  
    @Override
    public void visitAssignment(Object node) {
        // Обработка присваивания
    }
}
```

#### **`CodeGenException.java`** - Исключения кодогенерации

```java
package compiler.codegen;

public class CodeGenException extends RuntimeException {
    public CodeGenException(String message) {
        super(message);
    }
}
```

#### **`CodeGenTest.java`** - Модульные тесты кодогенератора

```java
package compiler.codegen;

public class CodeGenTest {
    public static void testWasmType() {
        // Тестирование типов WASM
    }
  
    public static void testWasmOperator() {
        // Тестирование операторов
    }
  
    public static void testCodeGenSymbolTable() {
        // Тестирование таблицы символов
    }
}
```

#### **`CodeGenUtils.java`** - Утилиты кодогенерации

```java
package compiler.codegen;

public class CodeGenUtils {
    public static String toWasmType(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer" -> "i32";
            case "real" -> "f64";
            case "boolean" -> "i32";
            case "array", "record" -> "i32";
            default -> throw new CodeGenException("Unknown language type: " + langType);
        };
    }
  
    public static String generateLabel(String prefix) {
        // Генерация уникальной метки
    }
}
```

#### **`MemoryLayout.java`** - Управление памятью

```java
package compiler.codegen;

public class MemoryLayout {
    private static final int HEAP_START = 0x1000;
    private int heapPointer = HEAP_START;
  
    public class RecordLayout {
        public String recordName;
        public int totalSize;
        public Map<String, Integer> fieldOffsets = new LinkedHashMap<>();
    
        public void addField(String fieldName, String fieldType) {
            // Добавление поля записи
        }
    }
  
    public int allocateArray(int elementCount, String elementType) {
        // Аллокация массива
    }
}
```

#### **`SymbolInfo.java`** - Информация о символе

```java
package compiler.codegen;

public class SymbolInfo {
    private String name;
    private String type;
    private SymbolKind kind;
    private int wasmIndex;
    private int memoryOffset;
    private boolean isParameter;

    public enum SymbolKind {
        LOCAL, GLOBAL, PARAMETER, FUNCTION, TYPE
    }
}
```

#### **`WasmOperator.java`** - Отображение операторов на WASM

```java
package compiler.codegen;

public class WasmOperator {
    public static String getBinaryOp(String operator, String operandType) {
        return switch (operator) {
            case "+" -> operandType.equals("f64") ? "f64.add" : "i32.add";
            case "-" -> operandType.equals("f64") ? "f64.sub" : "i32.sub";
            case "*" -> operandType.equals("f64") ? "f64.mul" : "i32.mul";
            case "/" -> operandType.equals("f64") ? "f64.div" : "i32.div_s";
            case "mod" -> "i32.rem_s";
            case "<" -> operandType.equals("f64") ? "f64.lt" : "i32.lt_s";
            // ... остальные операторы
        };
    }
}
```

#### **`WasmPrinter.java`** - Форматированный вывод WAT

```java
package compiler.codegen;

public class WasmPrinter {
    private StringBuilder output;
    private int indentLevel = 0;
    private static final String INDENT = "  ";
  
    public void startModule() {
        writeLine("(module");
        indent();
    }
  
    public void endModule() {
        dedent();
        writeLine(")");
    }
  
    public void startFunction(String name, String parameters, String returnType) {
        // Генерация заголовка функции
    }
}
```

#### **`WasmType.java`** - Типы WebAssembly

```java
package compiler.codegen;

public enum WasmType {
    I32("i32"),
    I64("i64"),
    F32("f32"),
    F64("f64");
  
    private final String wasmName;
  
    WasmType(String wasmName) {
        this.wasmName = wasmName;
    }
  
    public static WasmType fromLanguageType(String langType) {
        return switch (langType.toLowerCase()) {
            case "integer" -> I32;
            case "real" -> F64;
            case "boolean" -> I32;
            case "array", "record" -> I32;
            default -> throw new CodeGenException("Unknown language type: " + langType);
        };
    }
}
```

#### **`README.md`** - Документация кодогенератора

```
Содержание:
- Архитектура кодогенератора
- Примеры генерации кода
- Система типов WASM
- Управление памятью
- Интеграция с C++ парсером
```

---

## **🔧 C++ КОМПОНЕНТЫ**

### **Парсер и AST**

#### **`parser.y`** - Грамматика Bison

```yacc
%{
#include <iostream>
#include <vector>
#include <string>
#include <memory>
#include "ast.h"
#include "symbol.h"
#include "analyzer.h"

extern int yylex();
extern char* yytext;
extern int yylineno;
extern void yyerror(const char* msg);

bool hasParseError = false;
SymbolTable* symbolTable;
ProgramNode* astRoot;

JavaLexer* javaLexer;
%}

// Токены
%token TOK_IDENTIFIER TOK_STRING_LITERAL TOK_INTEGER_LITERAL TOK_REAL_LITERAL
%token TOK_VAR TOK_TYPE TOK_IS TOK_INTEGER TOK_REAL TOK_BOOLEAN
// ... остальные токены

%%
// Грамматические правила
program: declarations statements { 
    astRoot = new ProgramNode();
    // Добавление объявлений и операторов
    // Запуск семантического анализатора
    Analyzer analyzer;
    auto result = analyzer.analyze(astRoot);
}
;
```

**Особенности:**

- Более 800 строк грамматики
- Интеграция с семантическим анализатором
- Поддержка всех конструкций языка I
- Обработка ошибок парсинга
- Генерация AST Tree Printer

#### **`ast.h`** / **`ast.cpp`** - Определения AST узлов

```cpp
// Базовый класс AST
class ASTNode {
public:
    virtual ~ASTNode() = default;
};

// Типы
class TypeNode : public ASTNode {
    // Базовый класс для типов
};

class PrimitiveTypeNode : public TypeNode {
public:
    TypeKind kind;  // INTEGER, REAL, BOOLEAN
    PrimitiveTypeNode(TypeKind k) : kind(k) {}
};

class ArrayTypeNode : public TypeNode {
public:
    ExpressionNode* size;
    TypeNode* elementType;
    ArrayTypeNode(ExpressionNode* s, TypeNode* et) : size(s), elementType(et) {}
};

// Выражения
class ExpressionNode : public ASTNode {
public:
    TypeNode* type;
    ExpressionNode(TypeNode* t = nullptr) : type(t) {}
    virtual ~ExpressionNode() = default;
};

class IntegerLiteralNode : public ExpressionNode {
public:
    int value;
    IntegerLiteralNode(int v) : ExpressionNode(new PrimitiveTypeNode(TypeKind::INTEGER)), value(v) {}
};

class BinaryOpNode : public ExpressionNode {
public:
    OpKind op;
    ExpressionNode* left;
    ExpressionNode* right;
    BinaryOpNode(OpKind o, ExpressionNode* l, ExpressionNode* r) : op(o), left(l), right(r) {}
    ~BinaryOpNode() {
        delete left;
        delete right;
        delete type;
    }
};

// Операторы
class AssignmentNode : public StatementNode {
public:
    ExpressionNode* target;
    ExpressionNode* value;
    AssignmentNode(ExpressionNode* t, ExpressionNode* v) : target(t), value(v) {}
    ~AssignmentNode() {
        delete target;
        delete value;
    }
};

class IfStatementNode : public StatementNode {
public:
    ExpressionNode* condition;
    ASTNode* thenBody;
    ASTNode* elseBody;
    IfStatementNode(ExpressionNode* cond, ASTNode* tb, ASTNode* eb) : condition(cond), thenBody(tb), elseBody(eb) {}
    ~IfStatementNode() {
        delete condition;
        delete thenBody;
        delete elseBody;
    }
};

// Программа
class ProgramNode : public ASTNode {
public:
    std::vector<DeclarationNode*> declarations;
    std::vector<StatementNode*> statements;
  
    void addDeclaration(DeclarationNode* decl) { declarations.push_back(decl); }
    void addStatement(StatementNode* stmt) { statements.push_back(stmt); }
  
    ~ProgramNode() {
        for (auto decl : declarations) delete decl;
        for (auto stmt : statements) delete stmt;
    }
};
```

#### **`lexer.l`** - Лексер Flex

```flex
%{
#include <iostream>
#include <string>
#include <cstdlib>
#include "ast.h"
#include "parser.tab.h"

extern char* yytext;
extern int yylineno;
%}

%option noyywrap

%%

// Пробелы и комментарии
[ \t\r]+        ;  // игнорировать пробелы
\n              { yylineno++; }

// Ключевые слова
"var"           { return TOK_VAR; }
"integer"       { return TOK_INTEGER; }
"real"          { return TOK_REAL; }
"boolean"       { return TOK_BOOLEAN; }
// ... остальные ключевые слова

// Операторы
":="            { return TOK_ASSIGN; }
"+"             { return TOK_PLUS; }
"-"             { return TOK_MINUS; }
// ... остальные операторы

// Литералы
[0-9]+          { yylval.intVal = atoi(yytext); return TOK_INTEGER_LITERAL; }
[0-9]+\.[0-9]+  { yylval.realVal = atof(yytext); return TOK_REAL_LITERAL; }
\"[^\"]*\"       { yylval.strVal = strdup(yytext); return TOK_STRING_LITERAL; }

// Идентификаторы
[a-zA-Z_][a-zA-Z0-9_]* { yylval.strVal = strdup(yytext); return TOK_IDENTIFIER; }

<<EOF>>         { return 0; }

.               { std::cerr << "Unknown character: " << yytext << std::endl; }

%%
```

### **Семантический анализатор**

#### **`analyzer.h`** / **`analyzer.cpp`** - Семантический анализатор

```cpp
class Analyzer {
public:
    struct Result {
        std::vector<std::string> errors;
        std::vector<std::string> warnings;
        size_t optimizationsApplied = 0;
        bool success() const { return errors.empty(); }
    };

    explicit Analyzer(bool enableOptimizations = true)
        : enableOpts(enableOptimizations) {}

    Result analyze(ProgramNode* root);

private:
    bool enableOpts;
    Result result;

    // Проверки (без изменения AST)
    void runChecks(ProgramNode* root);
    void checkNode(ASTNode* node);
    void checkExpression(ExpressionNode* expr);
    void checkStatement(StatementNode* stmt);
    void checkRecordFieldAccess(FieldAccessNode* field);
    void checkArrayIndex(ArrayAccessNode* arrAcc);
    void checkRoutineCallTypes(const std::string& name, ASTNode* arguments);

    // Оптимизации (с изменением AST)
    void runOptimizations(ProgramNode* root);
    ExpressionNode* foldExpression(ExpressionNode* expr);
    void simplifyInBody(BodyNode* body);
    void simplifyInProgram(ProgramNode* program);
    void removeUnusedDeclarations(ProgramNode* program);
    void removeUnusedDeclarationsInBody(BodyNode* body, const std::unordered_set<std::string>& used);
    void collectUsedVariables(ASTNode* node, std::unordered_set<std::string>& used);
};
```

**Проверки (не изменяют AST):**

- Типы условий в циклах/ветвлениях
- Корректность вызовов функций (арность, типы аргументов)
- Доступ к полям записей
- Индексация массивов (тип индекса, границы)
- Возвращаемые типы функций

**Оптимизации (изменяют AST):**

- Свертка констант (constant folding)
- Упрощение условий (if true/false)
- Удаление мертвого кода (while false)
- Удаление неиспользуемых переменных

### **Таблица символов**

#### **`symbol.h`** / **`symbol.cpp`** - Таблица символов C++

```cpp
// Утилитарные функции
TypeNode* inferType(ExpressionNode* expr);
bool isRealType(TypeNode* type);
bool isBooleanType(TypeNode* type);
bool typesCompatible(TypeNode* t1, TypeNode* t2);

class VariableInfo {
public:
    std::string name;
    TypeNode* type;
    bool isUsed = false;
    // ... другие поля
};

class RoutineInfo {
public:
    std::string name;
    std::vector<TypeNode*> paramTypes;
    TypeNode* returnType;
    // ... другие поля
};

class SymbolTable {
private:
    std::vector<std::map<std::string, VariableInfo>> scopes;
    std::map<std::string, RoutineInfo> routines;
  
public:
    void enterScope();
    void exitScope();
    void declareVariable(const std::string& name, TypeNode* type);
    void declareRoutine(const std::string& name, const std::vector<TypeNode*>& paramTypes, TypeNode* returnType);
    VariableInfo* lookupVariable(const std::string& name);
    RoutineInfo* lookupRoutine(const std::string& name);
};
```

### **JNI интеграция**

#### **`codegen_bridge.cpp`** - JNI мост для кодогенерации

```cpp
// Глобальный указатель на AST
extern ProgramNode* astRoot;
extern SymbolTable* symbolTable;

// Преобразование AST в JSON
static std::string astNodeToJson(ASTNode* node, int depth = 0) {
    if (!node) return "null";
  
    std::stringstream json;
    std::string indent(depth * 2, ' ');
  
    if (auto* program = dynamic_cast<ProgramNode*>(node)) {
        json << "{\n";
        json << indent << "  \"type\": \"program\",\n";
        json << indent << "  \"declarations\": [\n";
    
        for (size_t i = 0; i < program->declarations.size(); ++i) {
            json << indent << "    " << astNodeToJson(program->declarations[i], depth + 2);
            if (i < program->declarations.size() - 1) json << ",";
            json << "\n";
        }
    
        json << indent << "  ],\n";
        json << indent << "  \"statements\": [\n";
    
        for (size_t i = 0; i < program->statements.size(); ++i) {
            json << indent << "    " << astNodeToJson(program->statements[i], depth + 2);
            if (i < program->statements.size() - 1) json << ",";
            json << "\n";
        }
    
        json << indent << "  ]\n";
        json << indent << "}\n";
    
    } else if (auto* varDecl = dynamic_cast<VariableDeclarationNode*>(node)) {
        json << "{\"type\": \"variable\", \"name\": \"" << varDecl->name << "\"";
        json << ", \"varType\": \"integer\"";  // Упрощенно
        json << "}";
    }
    // ... остальные типы узлов
  
    return json.str();
}

// JNI методы
extern "C" JNIEXPORT jlong JNICALL Java_compiler_codegen_CppASTBridge_getASTPointer
  (JNIEnv *env, jobject obj) {
    return reinterpret_cast<jlong>(astRoot);
}

extern "C" JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_getASTAsJson
  (JNIEnv *env, jobject obj, jlong astPointer) {
    ProgramNode* ast = reinterpret_cast<ProgramNode*>(astPointer);
    std::string json = astNodeToJson(ast);
    return env->NewStringUTF(json.c_str());
}
```

#### **`lexer.h`** / **`lexer.cpp`** - C++ обертка лексера

```cpp
class JavaLexer {
private:
    int lastToken;
    std::string lastLexeme;
    int lastLine;
  
public:
    JavaLexer();
    ~JavaLexer();
  
    // Методы для Flex лексера
    int nextToken();
    const char* getLexeme();
    int getType();
    int getLine();
  
    // Настройка входа
    void setInputFile(const char* filename);
    void setInputString(const char* input);
};
```

#### **`jni_lexer.cpp`** - JNI методы для лексера

```cpp
// Глобальный экземпляр лексера
JavaLexer* globalLexer = nullptr;

// JNI методы
extern "C" JNIEXPORT void JNICALL Java_compiler_lexer_Lexer_initializeParser
  (JNIEnv *env, jobject obj) {
    if (globalLexer == nullptr) {
        globalLexer = new JavaLexer();
    }
}

extern "C" JNIEXPORT jboolean JNICALL Java_compiler_lexer_Lexer_parseInput
  (JNIEnv *env, jobject obj, jstring input) {
    const char* inputStr = env->GetStringUTFChars(input, nullptr);
    globalLexer->setInputString(inputStr);
  
    // Запуск парсера
    int result = yyparse();
  
    env->ReleaseStringUTFChars(input, inputStr);
    return result == 0;
}
```

### **Сборка и конфигурация**

#### **`Makefile`** - Сборка C++ компонентов

```makefile
CXX = g++
CXXFLAGS = -std=c++11 -Wall -Wextra -g
BISON = bison
FLEX = flex

# Исходные файлы
BISON_SRC = parser.y
LEX_SRC = lexer.l
AST_SRC = ast.cpp
SYMBOL_SRC = symbol.cpp
ANALYZER_SRC = analyzer.cpp
LEXER_SRC = lexer.cpp
JNI_SRC = jni_lexer.cpp
CODEGEN_SRC = codegen_bridge.cpp

# Сгенерированные файлы
BISON_C = parser.tab.c
BISON_H = parser.tab.h
LEX_C = lex.yy.c
JNI_H = compiler_lexer_Lexer.h

# Объектные файлы
BISON_OBJ = parser.tab.o
LEX_OBJ = lex.yy.o
AST_OBJ = ast.o
# ... остальные объектные файлы

# Исполняемый файл и библиотека
TARGET = libparser.so
EXECUTABLE = parser

# JNI заголовки
JNI_INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I.

all: $(JNI_H) $(TARGET) $(EXECUTABLE)

$(EXECUTABLE): $(BISON_OBJ) $(LEX_OBJ) $(AST_OBJ) $(SYMBOL_OBJ) $(ANALYZER_OBJ) $(LEXER_OBJ) $(JNI_OBJ) $(CODEGEN_OBJ)
	$(CXX) $(CXXFLAGS) -fPIC -o $@ $^ $(JNI_INCLUDES)

$(TARGET): $(BISON_OBJ) $(LEX_OBJ) $(AST_OBJ) $(SYMBOL_OBJ) $(ANALYZER_OBJ) $(LEXER_OBJ) $(JNI_OBJ) $(CODEGEN_OBJ)
	$(CXX) $(CXXFLAGS) -shared -fPIC -Wl,--no-as-needed -o $@ $^ $(JNI_INCLUDES)

$(BISON_C) $(BISON_H): $(BISON_SRC)
	$(BISON) -d $<

$(LEX_C): $(LEX_SRC)
	$(FLEX) $<

$(JNI_H):
	@echo "JNI header file $(JNI_H) should be generated by javac -h"

%.o: %.c
	$(CXX) $(CXXFLAGS) -fPIC -c $< $(JNI_INCLUDES)

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -fPIC -c $< $(JNI_INCLUDES)

clean:
	rm -f $(BISON_C) $(BISON_H) $(LEX_C) $(JNI_H) *.o $(TARGET) $(EXECUTABLE)
```

#### **`parser.tab.h`** - Сгенерированные заголовки Bison

```cpp
/* A Bison parser, made by GNU Bison 3.8.2. */

#ifndef YY_YY_PARSER_TAB_H_INCLUDED
# define YY_YY_PARSER_TAB_H_INCLUDED

/* Token kinds. */
#ifndef YYTOKENTYPE
# define YYTOKENTYPE
  enum yytokentype
  {
    YYEMPTY = -2,
    YYEOF = 0,
    YYerror = 256,
    YYUNDEF = 257,
    TOK_IDENTIFIER = 258,
    TOK_STRING_LITERAL = 259,
    TOK_INTEGER_LITERAL = 260,
    TOK_REAL_LITERAL = 261,
    TOK_VAR = 262,
    // ... остальные токены
  };
#endif

// YYSTYPE определение
union YYSTYPE
{
    int intVal;
    double realVal;
    char* strVal;
    class ASTNode* astNode;
    // ... другие типы
};

extern YYSTYPE yylval;

int yyparse (void);

#endif /* !YY_YY_PARSER_TAB_H_INCLUDED */
```

#### **`compiler_codegen_CppASTBridge.h`** - Сгенерированный JNI заголовок

```cpp
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class compiler_codegen_CppASTBridge */

#ifndef _Included_compiler_codegen_CppASTBridge
#define _Included_compiler_codegen_CppASTBridge
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_compiler_codegen_CppASTBridge_getASTPointer
  (JNIEnv *, jobject);

JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_getASTAsJson
  (JNIEnv *, jobject, jlong);

JNIEXPORT jstring JNICALL Java_compiler_codegen_CppASTBridge_generateWasmFromAST
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
```

---

## **🧪 ТЕСТОВАЯ ИНФРАСТРУКТУРА**

### **`TestAllCases.java`** - Генератор WAT файлов

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TestAllCases {
    private static class VariableInfo {
        String name;
        String type;
        int offset;
    
        VariableInfo(String name, String type, int offset) {
            this.name = name;
            this.type = type;
            this.offset = offset;
        }
    }
  
    public static void main(String[] args) {
        // Очистка предыдущих результатов
        // Генерация WAT для тестов
        // Компиляция и запуск через wasmtime
    }
  
    private static String compileImperativeCode(String sourceCode, String relativePath) {
        // Разбор исходного кода
        // Генерация WASM модуля
        // Возврат WAT строки
    }
}
```

**Функции:**

- `parseAndGenerateWasm()` - основной метод генерации
- `compileImperativeCode()` - компиляция кода в WAT
- `addPrintFunctions()` - генерация функций печати
- Разбор выражений и генерация WASM инструкций

### **`test_compiler.sh`** - Основной скрипт тестирования

```bash
#!/bin/bash

# Тестер выполнения WAT файлов через wasmtime
# Проверяет все WAT файлы в папке wat_output

# Функции
cleanup_temp_files() {
    rm -f hs_err_pid*.log
    rm -f test_*.wat test_*.log 2>/dev/null || true
}

detect_wasmtime() {
    # Автообнаружение wasmtime
}

compile_file() {
    # Компиляция файла
}

run_wat() {
    # Выполнение WAT файла
}

test_all() {
    # Тестирование всех тестов
}

run_single() {
    # Выполнение одного теста
}

show_help() {
    # Справка
}

main() {
    # Основная логика
}
```

### **`run_all_tests.sh`** - Массовое тестирование

```bash
#!/bin/bash

# Скрипт для генерации WAT файлов из всех тестов и запуска через wasmtime

# Переменные
WASM_CMD="./wasmtime-v22.0.0-x86_64-linux/wasmtime"
TOTAL_TESTS=0
SUCCESS_COMPILE=0
SUCCESS_RUNTIME=0

# Обработка всех .i файлов
while IFS= read -r test_file; do
    # Компиляция и запуск каждого теста
done < <(find tests -name "*.i" | sort)

# Вывод результатов
```

### **`generate_report.sh`** - Генерация отчетов

```bash
#!/bin/bash

echo "# ПОЛНЫЕ РЕЗУЛЬТАТЫ ВСЕХ 45 ТЕСТОВ" > results.md

# Функция для конверсии WAT имени в I путь
convert_wat_to_i_path() {
    # Конверсия путей
}

# Генерация отчета
```

### **`docker_test.sh`** - Docker тестирование

```bash
#!/usr/bin/env bash
set -euo pipefail

# Опции
VERBOSE_FLAG=""
SUITE_VAL=""
FILTER_VAL=""

# Парсинг аргументов
while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--verbose) VERBOSE_FLAG=" --verbose"; shift ;;
        --suite) SUITE_VAL="${2:-}"; shift 2 ;;
        --filter) FILTER_VAL="${2:-}"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 2 ;;
    esac
done

# Сборка и запуск в Docker
IMAGE="hmm-compiler-test:latest"
docker build -t "${IMAGE}" .
docker run --rm -t -v "${HOST_PATH}:/app" -w "/app" "${IMAGE}" -lc "..."
```

### **`Dockerfile`** - Конфигурация Docker

```dockerfile
# Dev/test container for building and running the compiler tests
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      build-essential \
      bison \
      flex \
      openjdk-21-jdk-headless \
      ca-certificates \
      git \
      bash \
 && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME for JNI header generation
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app
```

### **Тестовые файлы (`tests/cases/`)**

#### **Структура тестов:**

```
tests/cases/
├── analyzer/
│   ├── arrays/
│   │   ├── array_checks.i
│   │   └── array_checks.meta
│   ├── control_flow/
│   │   ├── const_and_control.i
│   │   └── const_and_control.meta
│   └── ...
├── parser/
│   ├── basics/
│   │   ├── print_single.i
│   │   └── print_single.meta
│   └── ...
```

#### **Примеры тестов:**

**`array_checks.i`**:

```
var numbers: array[3] integer;
var i: real is 1.0;
numbers[i] := 10;
numbers[4] := 20;
```

**`array_checks.meta`**:

```
# Analyzer: array index and bounds
parseErr=0
expect: error: Array index must be integer
expect: warning: Array index 4 out of bounds [1..3] (static)
```

### **Тестовый harness (`tests/harness/`)**

#### **`run.sh`** - Унифицированный тестер

```bash
#!/usr/bin/env bash
set -euo pipefail

# Unified test runner for parser/analyzer

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
PARSER_DIR="$ROOT_DIR/compiler/src/main/cpp/parser"
CASES_DIR="$ROOT_DIR/tests/cases"

run_one() {
    local suite="$1" file_i="$2"
    # Обработка одного теста
}

# Обработка всех тестов
for test_file in "$CASES_DIR"/*/*/*.i; do
    # Запуск теста
done
```

### **Gradle конфигурация тестов (`tests/build.gradle`)**

```gradle
plugins {
    id 'java'
}

group = 'compiler.hmm'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    implementation project(':compiler')
    implementation platform('org.junit:junit-bom:5.10.0')
    implementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
    useJUnitPlatform()
}
```

#### **`TestLexer.java`** - Тесты лексера

```java
package compiler.hmm;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class TestLexer {

    private List<Token> tokenize(String sourceCode) throws LexerException {
        Lexer lexer = new Lexer(new StringReader(sourceCode));
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = lexer.nextToken()).getType() != TokenType.EOF) {
            tokens.add(token);
        }
        tokens.add(token); // add EOF
        return tokens;
    }

    @Test
    public void testVariableDeclarations() throws LexerException {
        String sourceCode = """
            var x: integer is 42;
            var y: real is 3.14;
            var flag: boolean is true;
            var name is "test";""";

        List<Token> expected = Arrays.asList(
            new Token(TokenType.VAR, "var", 1, 1),
            new Token(TokenType.IDENTIFIER, "x", 1, 5),
            // ... остальные токены
            new Token(TokenType.EOF, "", 4, 20)
        );

        List<Token> actual = tokenize(sourceCode);
        assertEquals(expected, actual);
    }

    // ... остальные тесты
}
```

---

## **📄 ДОКУМЕНТАЦИЯ**

### **`README.md`** - Основная документация

- Команда разработки (Mikhail Trifonov, Kirill Efimovich)
- Архитектура компилятора
- Инструкции по сборке и запуску
- Спецификация языка I
- Примеры кода и тестов

### **`QUICKSTART.md`** - Быстрый старт

- Команды сборки (Gradle + Make)
- Тестирование в Docker
- Поддерживаемые возможности языка

### **`docs/analyzer.md`** - Семантический анализатор

```
Описание: Проверки и оптимизации анализатора
Примеры: Постоянная свертка, упрощение if, удаление while false
Запуск: Автоматический после парсинга
```

### **`docs/codegen_implementation.md`** - Кодогенерация

```
Архитектура: WasmCodeGenerator, CodeGenVisitor, SymbolTable
Примеры: Генерация WAT для переменных, циклов, массивов
Интеграция: JNI мост к C++ AST
```

### **`docs/testing_guide.md`** - Руководство по тестированию

```
Компоненты: Java лексер, C++ парсер, интеграционные тесты
Примеры: Запуск тестов, интерпретация результатов
```

### **`docs/lexer_scope.md`** - Спецификация лексера

```
FSM: Конечный автомат лексера
Токены: Полный список поддерживаемых токенов
```

### **`docs/parser_scope.md`** - Спецификация парсера

```
Грамматика: BNF грамматика языка I
AST: Структура абстрактного синтаксического дерева
```

### **`docs/analyzer_tests.md`** - Тесты анализатора

```
Категории: Constant folding, control flow, arrays, records
Примеры: Тестовые случаи с ожидаемыми результатами
```

### **`docs/docker-testing.md`** - Тестирование в Docker

```
Преимущества: Изоляция, консистентность
Команды: Сборка образов, запуск тестов
```

### **`docs/COMPILER_INTEGRATION.md`** - Интеграция компонентов

```
Архитектура: Java ↔ C++ через JNI
Протокол: Передача AST, генерация кода
```

### **Другие документы:**

- `slides*.md/pdf` - Презентации и слайды
- `Projec I.md` - Описание проекта I (оригинальный язык)

---

## **🚀 СКРИПТЫ ЗАПУСКА**

### **`gradlew`** / **`gradlew.bat`** - Gradle wrapper

```
Функции: Кросс-платформенный запуск Gradle без установки
```

### **`final_report.md`** - Финальный отчет

```
Содержание: Результаты всех тестов, анализ производительности
```

### **`results.md`** - Результаты тестирования

```
Генерируется: generate_report.sh
```

### **Логи и отчеты:**

- `all_test_errors.txt` - Все ошибки тестов
- `failing_tests.txt` - Провалившиеся тесты
- `final_result.txt` - Финальные результаты
- `wasmtime_full_report.txt` - Отчет wasmtime

---

## **🎯 ВЫХОДНЫЕ ФАЙЛЫ (WebAssembly)**

### **`wat_output/`** - Сгенерированные WAT файлы

Пример структуры WAT модуля:

```wasm
(module
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
  (memory 1)
  (export "memory" (memory 0))
  (global $heap_ptr (mut i32) (i32.const 0x10000))
  (global $print_buffer i32 (i32.const 0x1000))
  (global $iovec_buffer i32 (i32.const 0x1010))
  (global $nwritten i32 (i32.const 0x1020))

  (func $init_print_buffer
  )

  (func $alloc (param $size i32) (result i32)
    global.get $heap_ptr
    global.get $heap_ptr
    local.get $size
    i32.add
    global.set $heap_ptr
  )

  (func $print_int (param $n i32)
    ;; Реализация печати целых чисел
  )

  (func $print_char (param $char i32)
    ;; Реализация печати символов через WASI
  )

  (func $_start
    call $init_print_buffer
    ;; Код программы
  )
  (export "_start" (func $_start))
)
```

---

## **🏗 КЛЮЧЕВЫЕ ОСОБЕННОСТИ ПРОЕКТА**

1. **Гибридная архитектура**: Java лексер + C++ парсер/анализатор + Java кодогенератор
2. **Полный цикл компиляции**: Исходный код → Токены → AST → Семантический анализ → WASM
3. **Семантический анализ**: Статические проверки + оптимизации (свертка констант, удаление мертвого кода)
4. **WASI интеграция**: Исполнение в wasmtime с вводом/выводом
5. **Комплексное тестирование**: 45+ тестов с автоматической проверкой
6. **JNI интеграция**: Бесшовная связь между Java и C++ компонентами
7. **Документированность**: Подробные спецификации и руководства
8. **Кросс-платформенность**: Docker для консистентного тестирования

**Проект демонстрирует полный цикл разработки современного компилятора от лексического анализа до генерации исполняемого кода WebAssembly.**
