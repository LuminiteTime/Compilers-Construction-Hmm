// Test: Type conversions between primitive types
var int_val : integer is 42
var real_val : real is 3.14
var bool_val : boolean is true

// integer to real
var int_to_real : real is int_val

// integer to boolean (non-zero -> true, zero -> false)
var int_to_bool1 : boolean is 5    // true
var int_to_bool2 : boolean is 0    // false

// real to integer (truncation)
var real_to_int : integer is real_val

// boolean to integer
var bool_to_int1 : integer is true   // 1
var bool_to_int2 : integer is false  // 0

// boolean to real
var bool_to_real1 : real is true    // 1.0
var bool_to_real2 : real is false   // 0.0

print int_to_real
print int_to_bool1
print int_to_bool2
print real_to_int
print bool_to_int1
print bool_to_real1
