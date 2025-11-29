// Test: Nested control structures
var result is 0

for i in 1..3 loop
  var inner_sum is 0
  for j in 1..i loop
    inner_sum := inner_sum + j
  end

  if inner_sum > 3 then
    result := result + inner_sum
  else
    result := result + 1
  end
end

print result  // Should be 1 + 1 + 6 = 8
