// Test: Mixed variable declarations
var a : integer is 5
var b is a + 10        // type inferred from expression
var c : integer        // explicit type, no initializer
c := 15                // assignment after declaration

print a
print b
print c
