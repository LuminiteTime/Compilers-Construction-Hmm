// Test: Function calls in expressions
routine double(x : integer) : integer is
  return x * 2
end

routine is_even(n : integer) : boolean is
  return n % 2 = 0
end

var result1 is double(5) + 10
var result2 is is_even(double(3))
var result3 is double(2) > 3 and is_even(4)

print result1
print result2
print result3
