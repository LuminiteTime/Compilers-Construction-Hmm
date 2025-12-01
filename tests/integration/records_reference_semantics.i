// Integration test: reference semantics for records and arrays

type Pair is record
  var a : integer is 0
  var b : integer is 0
end

var p1 : Pair
p1.a := 1
p1.b := 2

var p2 : Pair
p2 := p1  // copy reference to the same Pair object

// Mutate through p1 and observe changes via p2
p1.a := 10
p1.b := 20

print p2.a   // 10
print p2.b   // 20

var arr1 : array [3] integer
arr1[1] := 1
arr1[2] := 2
arr1[3] := 3

var arr2 : array [3] integer
arr2 := arr1  // copy reference to the same array object

// Mutate through arr1 and observe via arr2
arr1[2] := 99

print arr2[1]  // 1
print arr2[2]  // 99
print arr2[3]  // 3

// Expected numeric output (ignoring spaces/newlines): 10201993
