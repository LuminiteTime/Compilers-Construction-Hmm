# Makefile for Bison Parser

CXX = g++
CXXFLAGS = -std=c++11 -Wall -Wextra -g
BISON = bison
FLEX = flex

# Source files
BISON_SRC = parser.y
LEX_SRC = lexer.l
AST_SRC = ast.cpp
SYMBOL_SRC = symbol.cpp
LEXER_SRC = lexer.cpp

# Generated files
BISON_C = parser.tab.c
BISON_H = parser.tab.h
LEX_C = lex.yy.c

# Object files
BISON_OBJ = parser.tab.o
LEX_OBJ = lex.yy.o
AST_OBJ = ast.o
SYMBOL_OBJ = symbol.o
LEXER_OBJ = lexer.o

# Executable
TARGET = parser

# JNI includes (adjust path as needed)
JNI_INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

all: $(TARGET)

$(TARGET): $(BISON_OBJ) $(LEX_OBJ) $(AST_OBJ) $(SYMBOL_OBJ) $(LEXER_OBJ)
	$(CXX) $(CXXFLAGS) -o $@ $^ $(JNI_INCLUDES) -ljni

$(BISON_C) $(BISON_H): $(BISON_SRC)
	$(BISON) -d $<

$(LEX_C): $(LEX_SRC)
	$(FLEX) $<

%.o: %.c
	$(CXX) $(CXXFLAGS) -c $< $(JNI_INCLUDES)

%.o: %.cpp
	$(CXX) $(CXXFLAGS) -c $< $(JNI_INCLUDES)

ast.o: ast.h
symbol.o: symbol.h ast.h
lexer.o: lexer.h

clean:
	rm -f $(BISON_C) $(BISON_H) $(LEX_C) *.o $(TARGET)

.PHONY: all clean