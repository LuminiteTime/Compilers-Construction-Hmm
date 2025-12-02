var arr : array [5] integer
arr[1] := 5
arr[2] := 1
arr[3] := -36
arr[4] := 12
arr[5] := 3

// Print unsorted array: 5 1 4 2 3
for k in 1..5 loop
  print arr[k]
end
print 10000001

var i : integer is 1
while i <= 4 loop
  var j : integer is 1
  while j <= 5 - i loop
    if arr[j] > arr[j + 1] then
      var tmp : integer is arr[j]
      arr[j] := arr[j + 1]
      arr[j + 1] := tmp
    end
    j := j + 1
  end
  i := i + 1
end

// Print sorted array: 1 2 3 4 5
for j in 1..5 loop
  print arr[j]
end
