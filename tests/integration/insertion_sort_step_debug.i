// Step-debug version of insertion_sort: print i, j and array after each inner iteration
var arr : array [5] integer
arr[1] := 5
arr[2] := 1
arr[3] := 4
arr[4] := 2
arr[5] := 3

// Print initial array
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

    // Debug output for this inner iteration
    print i
    print j
    for k in 1..5 loop
      print arr[k]
    end
  end
  arr[j + 1] := key

  // Debug after placing key
  print i
  print 0
  for k in 1..5 loop
    print arr[k]
  end

  i := i + 1
end

// Final array
for k in 1..5 loop
  print arr[k]
end
