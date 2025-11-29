// Test: For loop with range
for i in 1..5 loop
  print i
end

// Test: For loop with reverse
for j in 5..1 reverse loop
  print j
end

// Test: For loop with variables in range
var start is 2
var end is 4

for k in start..end loop
  print k
end

// Test: Empty range (should not execute)
for m in 5..2 loop
  print 999  // should not print
end
