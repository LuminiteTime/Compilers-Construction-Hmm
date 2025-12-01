// Integration test: complex boolean logic expressions

var a : boolean is true
var b : boolean is false
var c : boolean is true

// Expression: (a and not b) or (b xor c)
var r1 : boolean is (a and not b) or (b xor c)

// Expression: (a or b) and (c xor b)
var r2 : boolean is (a or b) and (c xor b)

// Print as booleans (true/false)
print r1
print r2
