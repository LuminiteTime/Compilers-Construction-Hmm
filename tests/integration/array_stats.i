// Integration test: statistics on an integer array (sum, min, max, average)

routine sum_array(a : array [5] integer) : integer
is
  var s : integer is 0
  for i in 1..5 loop
    s := s + a[i]
  end
  return s
end

routine min_array(a : array [5] integer) : integer
is
  var m : integer is a[1]
  for i in 2..5 loop
    if a[i] < m then
      m := a[i]
    end
  end
  return m
end

routine max_array(a : array [5] integer) : integer
is
  var m : integer is a[1]
  for i in 2..5 loop
    if a[i] > m then
      m := a[i]
    end
  end
  return m
end

var arr : array [5] integer
arr[1] := 3
arr[2] := 7
arr[3] := 1
arr[4] := 9
arr[5] := 5

var s : integer is sum_array(arr)
var mn : integer is min_array(arr)
var mx : integer is max_array(arr)
var avg : real is s / 5

print s   // 25
print mn  // 1
print mx  // 9
print avg // 5.0
