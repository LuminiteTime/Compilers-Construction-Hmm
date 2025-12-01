// Integration test: short-body routine syntax with => Expression

routine inc(x : integer) : integer => x + 1

var a : integer is inc(5)
var b : integer is inc(0)

// Expected numeric output (ignoring spaces/newlines): 61
print a   // 6
print b   // 1
