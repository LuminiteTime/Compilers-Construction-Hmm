var global is 42

routine test() is
  var local is 24
  print global  // should work
  print local   // should work
end

print global    // should work
print local     // should error
