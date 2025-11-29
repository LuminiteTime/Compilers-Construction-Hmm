// Test: Multidimensional arrays (matrices)
var matrix : array [3] array [3] integer

// Initialize matrix
for i in 1..3 loop
  for j in 1..3 loop
    matrix[i][j] := i * j
  end
end

// Print diagonal elements
print matrix[1][1]  // 1
print matrix[2][2]  // 4
print matrix[3][3]  // 9

// Test: Matrix operations
var sum is 0
for x in 1..3 loop
  sum := sum + matrix[x][x]  // diagonal sum
end

print sum  // Should be 1 + 4 + 9 = 14
