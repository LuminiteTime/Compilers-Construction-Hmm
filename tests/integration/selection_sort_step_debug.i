// Step-debug version of selection_sort: print i, j, minIndex and array
var arr : array [5] integer
arr[1] := 3
arr[2] := 1
arr[3] := 5
arr[4] := 2
arr[5] := 4

// Print initial array
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

    // Debug after each inner iteration
    print i
    print j
    print minIndex
    for k in 1..5 loop
      print arr[k]
    end

    j := j + 1
  end

  if minIndex /= i then
    var tmp : integer is arr[i]
    arr[i] := arr[minIndex]
    arr[minIndex] := tmp
  end

  // Debug after potential swap for this i
  print i
  print 0
  print minIndex
  for k in 1..5 loop
    print arr[k]
  end

  i := i + 1
end

// Final array
for k in 1..5 loop
  print arr[k]
end
