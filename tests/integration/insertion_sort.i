// Insertion sort test on small integer array
var arr : array [5] integer
arr[1] := 5
arr[2] := 1
arr[3] := 4
arr[4] := 2
arr[5] := 3

// Print unsorted array: 5 1 4 2 3
for k in 1..5 loop
  print arr[k]
end

// Insertion sort ascending
var i : integer is 2
while i <= 5 loop
  var key : integer is arr[i]
  var j : integer is i - 1
  while j >= 1 loop
    if arr[j] > key then
      arr[j + 1] := arr[j]
      j := j - 1
    else
      j := 0
    end
  end
  arr[j + 1] := key
  i := i + 1
end

// Print sorted array: 1 2 3 4 5
for k in 1..5 loop
  print arr[k]
end
