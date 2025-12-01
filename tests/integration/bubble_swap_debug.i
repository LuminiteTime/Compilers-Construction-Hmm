// Debug swap for bubble sort
var arr : array [5] integer
arr[1] := 5
arr[2] := 1
arr[3] := 4
arr[4] := 2
arr[5] := 3

var j : integer is 1
if arr[j] > arr[j + 1] then
  var tmp : integer is arr[j]
  arr[j] := arr[j + 1]
  arr[j + 1] := tmp
end

for k in 1..5 loop
  print arr[k]
end
