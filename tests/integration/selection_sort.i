// Selection sort test on small integer array
var arr : array [5] integer
arr[1] := 3
arr[2] := 1
arr[3] := 5
arr[4] := 2
arr[5] := 4

// Print unsorted array: 3 1 5 2 4
for k in 1..5 loop
  print arr[k]
end

// Selection sort ascending
var i : integer is 1
while i <= 4 loop
  var minIndex : integer is i
  var j : integer is i + 1
  while j <= 5 loop
    if arr[j] < arr[minIndex] then
      minIndex := j
    end
    j := j + 1
  end

  if minIndex /= i then
    var tmp : integer is arr[i]
    arr[i] := arr[minIndex]
    arr[minIndex] := tmp
  end

  i := i + 1
end

// Print sorted array: 1 2 3 4 5
for k in 1..5 loop
  print arr[k]
end
