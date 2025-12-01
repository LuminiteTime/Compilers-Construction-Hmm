// Integration test: 2x2 matrix multiplication
var A : array [2] array [2] integer
var B : array [2] array [2] integer
var C : array [2] array [2] integer

// A = [[1, 2], [3, 4]]
A[1][1] := 1
A[1][2] := 2
A[2][1] := 3
A[2][2] := 4

// B = [[2, 0], [1, 2]]
B[1][1] := 2
B[1][2] := 0
B[2][1] := 1
B[2][2] := 2

// C = A * B
for i in 1..2 loop
  for j in 1..2 loop
    C[i][j] := 0
    for k in 1..2 loop
      C[i][j] := C[i][j] + A[i][k] * B[k][j]
    end
  end
end

// Expected C = [[4, 4], [10, 8]]
print C[1][1]
print C[1][2]
print C[2][1]
print C[2][2]
