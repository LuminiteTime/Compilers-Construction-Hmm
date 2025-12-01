// Integration test: reverse an array in place
var arr : array [5] integer
arr[1] := 1
arr[2] := 2
arr[3] := 3
arr[4] := 4
arr[5] := 5

var i : integer is 1
var j : integer is 5

while i < j loop
  var tmp : integer is arr[i]
  arr[i] := arr[j]
  arr[j] := tmp
  i := i + 1
  j := j - 1
end

// Expected output: 5 4 3 2 1
for k in 1..5 loop
  print arr[k]
end
