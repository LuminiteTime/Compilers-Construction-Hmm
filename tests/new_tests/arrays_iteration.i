// Test: Array iteration with for loops
var numbers : array [5] integer

// Initialize array
for i in 1..5 loop
  numbers[i] := i * 10
end

// Test: Sum calculation
var sum : integer is 0
for k in 1..5 loop
  sum := sum + numbers[k]
end
// Sum calculation is tested by successful execution
