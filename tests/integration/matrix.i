var matrix : array [3] array [3] integer

for i in 1..3 loop
  for j in 1..3 loop
    matrix[i][j] := i * j
  end
end

print matrix[2][3]  // should be 6
