// Test: Functions with return values
routine add(a : integer, b : integer) : integer is
  return a + b
end

routine is_positive(x : integer) : boolean is
  return x > 0
end

routine square(x : real) : real is
  return x * x
end

// Test function calls
var sum is add(5, 3)
var positive is is_positive(-2)
var squared is square(3.5)

print sum      // 8
print positive // false
print squared  // 12.25
