// Integration test: variable shadowing across nested scopes

var x : integer is 1

routine f() : integer
is
  var x : integer is 2
  if x > 1 then
    var x : integer is 3
    return x
  end
  return x
end

var y : integer is f()

// Expected numeric output (ignoring spaces/newlines): 13
print x   // 1 (global)
print y   // 3 (from inner-most scope in f)
