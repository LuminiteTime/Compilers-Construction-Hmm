// Test: Basic record declaration and field access
type Point is record
  var x : integer is 0
  var y : integer is 0
end

var p1 : Point
var p2 : Point

p1.x := 10
p1.y := 20
p2.x := 5
p2.y := 15

print p1.x
print p1.y
print p2.x
print p2.y

// Test: Record field access in expressions
var distance is p1.x + p2.x
print distance  // 15
