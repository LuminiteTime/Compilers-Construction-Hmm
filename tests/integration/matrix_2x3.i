// Integration test: 2x3 matrix (non-square) to stress 2D indexing
var M : array [2] array [3] integer

// Fill M with values 1..6 in row-major order
for i in 1..2 loop
  for j in 1..3 loop
    M[i][j] := (i - 1) * 3 + j
  end
end

// Print all elements in row-major order: 1 2 3 4 5 6
for i in 1..2 loop
  for j in 1..3 loop
    print M[i][j]
  end
end
