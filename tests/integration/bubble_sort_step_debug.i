// Step-debug version of bubble_sort: print i, j and array after each inner iteration
var arr : array [5] integer
arr[1] := 5
arr[2] := 1
arr[3] := 4
arr[4] := 2
arr[5] := 3

var i : integer is 1
while i <= 4 loop
  var j : integer is 1
  while j <= 5 - i loop
    if arr[j] > arr[j + 1] then
      var tmp : integer is arr[j]
      arr[j] := arr[j + 1]
      arr[j + 1] := tmp
    end

    // Debug output for this inner iteration
    print i
    print j
    for k in 1..5 loop
      print arr[k]
    end

    j := j + 1
  end
  i := i + 1
end
