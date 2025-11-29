// Test: Various array type declarations
type IntArray is array [10] integer
type RealArray is array [5] real
type BoolArray is array [3] boolean

// Test: Direct array type usage
var arr1 : array [4] integer
var arr2 : array [2] array [3] integer  // 2D array

// Test: Dynamic arrays (for parameters)
var dynamic : array [] real

print 1
