// Test: Simple procedures (no return value)
routine print_number(n : integer) is
  print n
end

routine increment(x : integer) is
  var result is x + 1
  print result
end

// Call procedures
print_number(42)
increment(10)
