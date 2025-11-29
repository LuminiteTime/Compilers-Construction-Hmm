// Test: While loop
var counter is 0

while counter < 5 loop
  print counter
  counter := counter + 1
end

// Test: While loop with complex condition
var x is 10
var y is 0

while x > 0 and y < 3 loop
  y := y + 1
  x := x - 2
  print y
end
