// Test type inference
var x is 10        // Should infer integer
var y is 3.14      // Should infer real
var z is true      // Should infer boolean
var a is x + 5     // Should infer integer
var b is y * 2.0   // Should infer real
var c is x < 5     // Should infer boolean

// Test with explicit types
var explicit_int : integer is 42
var explicit_real : real is 2.71
var explicit_bool : boolean is false
