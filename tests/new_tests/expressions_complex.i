// Test: Complex nested expressions
var a is 2
var b is 3
var c is 4

var complex1 is (a + b) * c - (a / b) + c % a
var complex2 is a < b and (c > a or b = 3)
var complex3 is not (a >= b) and c <= 10 or a /= 0

// Test: Mixed arithmetic and logical
var mixed is (2 + 3) * 4 > 10 and 5 / 2 = 2

print complex1
print complex2
print complex3
print mixed
