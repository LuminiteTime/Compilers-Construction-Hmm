// Test forward declarations
routine add(a : integer, b : integer) : integer

var result is add(5, 3)

routine add(a : integer, b : integer) : integer
is
  return a + b
end
