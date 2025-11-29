// Test: Nested records
type Address is record
  var street : integer is 123  // using integer since no strings
  var city : integer is 456
end

type Person is record
  var name : integer is 789
  var age : integer is 25
  var addr : Address
end

var person : Person

person.age := 30
person.addr.street := 999
person.addr.city := 888

print person.age
print person.addr.street
print person.addr.city

// Test: Record assignment (reference copy)
var person2 : Person
person2 := person
person2.age := 35

print person.age   // should still be 30 (reference copy)
print person2.age  // should be 35
