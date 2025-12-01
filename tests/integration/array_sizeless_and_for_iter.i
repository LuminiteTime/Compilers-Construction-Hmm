// Integration test: sizeless array parameter and for-in array iteration (with reverse)

routine sum_all(a : array [] integer, n : integer) : integer
is
  var s : integer is 0
  var i : integer is 1
  while i <= n loop
    s := s + a[i]
    i := i + 1
  end
  return s
end

var arr5 : array [5] integer
arr5[1] := 1
arr5[2] := 2
arr5[3] := 3
arr5[4] := 4
arr5[5] := 5

// Sum via sizeless-parameter routine
var s1 : integer is sum_all(arr5, 5)

// Sum via for-in iteration over array (forward)
var s_for : integer is 0
for x in arr5 loop
  s_for := s_for + x
end

// Accumulate digits via for-in reverse iteration over array
var d_rev : integer is 0
for x in arr5 reverse loop
  d_rev := d_rev * 10 + x
end

// Expected numeric output (ignoring spaces/newlines): 151554321
print s1      // 15
print s_for   // 15
print d_rev   // 54321
