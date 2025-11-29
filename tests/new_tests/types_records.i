// Test: Record type declarations
type Point is record
  var x : integer is 0
  var y : integer is 0
end

type Person is record
  var age : integer is 25
  var height : real is 175.5
  var is_student : boolean is true
end

var p : Point
var person : Person

print p.x
print person.age
