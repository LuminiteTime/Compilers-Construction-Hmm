// Test: Scoping rules and variable shadowing
var global_var is 100

if true then
  var inner_var is 400
  print inner_var  // should work inside if block
end

print inner_var  // should error - not in scope
