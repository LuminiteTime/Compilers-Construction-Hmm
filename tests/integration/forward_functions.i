// Integration test: forward routine declarations

routine sum_three(a : integer, b : integer, c : integer) : integer

routine sum_two(a : integer, b : integer) : integer
is
  return a + b
end

routine sum_three(a : integer, b : integer, c : integer) : integer
is
  return sum_two(a, b) + c
end

var s : integer is sum_three(1, 2, 3)
print s  // 6
