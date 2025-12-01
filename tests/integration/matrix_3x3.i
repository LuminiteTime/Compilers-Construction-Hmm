// Integration test: 3x3 matrix, flattened 2D array
var A : array [3] array [3] integer

// Fill A with values 1..9 in row-major order
for i in 1..3 loop
  for j in 1..3 loop
    A[i][j] := (i - 1) * 3 + j
  end
end

// Print all elements in row-major order: 1 2 3 4 5 6 7 8 9
for i in 1..3 loop
  for j in 1..3 loop
    print A[i][j]
  end
end
