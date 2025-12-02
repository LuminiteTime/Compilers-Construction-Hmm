// Test: Comparison expressions
var eq is 5 = 5
var ne is 5 /= 4
var lt is 3 < 5
var le is 5 <= 5
var gt is 7 > 3
var ge is 5 >= 6

// Test: Logical expressions
var and_expr is true and false
var or_expr is true or false
var xor_expr is true xor false
var not_expr is not true

// Test: Complex logical expressions
var complex is (5 > 3) and (10 >= 8) or not (2 = 3)

print eq
print ne
print lt
print le
print gt
print ge
print and_expr
print or_expr
print xor_expr
print not_expr
print complex
