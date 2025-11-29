// Test: Functions with various parameter types
routine process_data(x : integer, y : real, flag : boolean) : integer is
  if flag then
    return x
  else
    return x + 1
  end
end

routine calculate(a : integer, b : integer, c : integer) : integer is
  return a * b + c
end

// Test function calls with different argument types
var result1 is process_data(10, 3.14, true)
var result2 is process_data(5, 2.71, false)
var result3 is calculate(2, 3, 4)

print result1  // 10
print result2  // 6
print result3  // 10
