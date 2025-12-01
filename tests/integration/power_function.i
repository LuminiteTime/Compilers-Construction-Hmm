// Integration test: iterative power function
routine power(base : integer, exp : integer) : integer
is
  var result : integer is 1
  var i : integer is 1
  while i <= exp loop
    result := result * base
    i := i + 1
  end
  return result
end

var p1 : integer is power(2, 5)
var p2 : integer is power(3, 4)

print p1  // 32
print p2  // 81
