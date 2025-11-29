routine add(a : integer, b : integer) : integer
is
  a + b
end

var result is add(5, 3)  // OK
var wrong is add(5)      // error: wrong number of args
var bad is add("hello", 3) // error: wrong types
