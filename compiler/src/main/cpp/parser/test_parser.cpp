#include <iostream>
#include <fstream>
#include <sstream>
#include "ast.h"
#include "symbol.h"

// Simple test program to check AST and symbol table functionality
int main() {
    std::cout << "Testing AST and Symbol Table functionality" << std::endl;

    // Test symbol table
    SymbolTable* symbolTable = new SymbolTable();

    // Add some test variables
    PrimitiveTypeNode* intType = new PrimitiveTypeNode(TypeKind::INTEGER);
    PrimitiveTypeNode* realType = new PrimitiveTypeNode(TypeKind::REAL);
    PrimitiveTypeNode* boolType = new PrimitiveTypeNode(TypeKind::BOOLEAN);

    symbolTable->declareVariable("x", intType);
    symbolTable->declareVariable("y", realType);
    symbolTable->declareVariable("flag", boolType);

    // Test lookup
    VariableInfo* varX = symbolTable->lookupVariable("x");
    if (varX) {
        std::cout << "Found variable x with type: " <<
            (dynamic_cast<PrimitiveTypeNode*>(varX->type) ? "INTEGER" : "UNKNOWN") << std::endl;
    }

    // Test type inference
    IntegerLiteralNode* intLit = new IntegerLiteralNode(42);
    TypeNode* inferredType = inferType(intLit);
    std::cout << "Inferred type for integer literal: " <<
        (dynamic_cast<PrimitiveTypeNode*>(inferredType) ?
         (dynamic_cast<PrimitiveTypeNode*>(inferredType)->kind == TypeKind::INTEGER ? "INTEGER" : "OTHER") :
         "UNKNOWN") << std::endl;

    // Test binary operation type inference
    BinaryOpNode* addOp = new BinaryOpNode(OpKind::PLUS, intLit, new IntegerLiteralNode(10));
    TypeNode* addType = inferType(addOp);
    std::cout << "Inferred type for addition: " <<
        (dynamic_cast<PrimitiveTypeNode*>(addType) ?
         (dynamic_cast<PrimitiveTypeNode*>(addType)->kind == TypeKind::INTEGER ? "INTEGER" : "OTHER") :
         "UNKNOWN") << std::endl;

    // Test AST creation
    ProgramNode* program = new ProgramNode();

    VariableDeclarationNode* varDecl = new VariableDeclarationNode("testVar", intType, intLit);
    program->addDeclaration(varDecl);

    std::cout << "Created program with " << program->declarations.size() << " declarations" << std::endl;

    // Cleanup
    delete program;
    delete symbolTable;
    delete intLit->type; // Clean up types created by literals
    delete addOp; // This will delete its children due to destructors

    std::cout << "All tests completed successfully!" << std::endl;
    return 0;
}
