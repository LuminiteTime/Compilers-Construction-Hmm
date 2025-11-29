var arr : array [5] integer
arr[1] := 1
arr[2] := 2
arr[3] := 3
arr[4] := 4
arr[5] := 5

var sum is 0
for i in 1..5 loop
  sum := sum + arr[i]
end

print sum
