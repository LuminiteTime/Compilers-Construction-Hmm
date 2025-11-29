// Test: Arithmetic expressions with precedence
var a is 2 + 3 * 4        // 2 + (3 * 4) = 14
var b is 10 - 2 * 3 + 5   // (10 - (2 * 3)) + 5 = 11
var c is 20 / 4 + 3       // (20 / 4) + 3 = 8
var d is 7 % 3 * 2        // (7 % 3) * 2 = 2
var e is (2 + 3) * 4      // parentheses override precedence

print a
print b
print c
print d
print e
