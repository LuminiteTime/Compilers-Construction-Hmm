// Integration test: 4x4 matrix, flattened 2D array
var A : array [4] array [4] integer

// Fill A with values 1..16 in row-major order
for i in 1..4 loop
  for j in 1..4 loop
    A[i][j] := (i - 1) * 4 + j
  end
end

// Print all elements in row-major order: 1..16
for i in 1..4 loop
  for j in 1..4 loop
    print A[i][j]
  end
end
