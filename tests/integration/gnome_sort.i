// Gnome sort test on small integer array
var arr : array [5] integer
arr[1] := 2
arr[2] := 5
arr[3] := 3
arr[4] := 1
arr[5] := 4

// Print unsorted array: 2 5 3 1 4
for k in 1..5 loop
  print arr[k]
end

// Gnome sort ascending
var pos : integer is 2
while pos <= 5 loop
  if arr[pos] >= arr[pos - 1] then
    pos := pos + 1
  else
    var tmp : integer is arr[pos]
    arr[pos] := arr[pos - 1]
    arr[pos - 1] := tmp
    if pos > 2 then
      pos := pos - 1
    else
      pos := 2
    end
  end
end

// Print sorted array: 1 2 3 4 5
for k in 1..5 loop
  print arr[k]
end
