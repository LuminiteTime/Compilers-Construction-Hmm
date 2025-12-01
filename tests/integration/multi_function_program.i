// Integration test: multiple small utility functions

routine min3(a : integer, b : integer, c : integer) : integer
is
  var m : integer is a
  if b < m then
    m := b
  end
  if c < m then
    m := c
  end
  return m
end

routine max3(a : integer, b : integer, c : integer) : integer
is
  var m : integer is a
  if b > m then
    m := b
  end
  if c > m then
    m := c
  end
  return m
end

routine abs_int(x : integer) : integer
is
  if x < 0 then
    return -x
  else
    return x
  end
end

var mn : integer is min3(3, 1, 2)
var mx : integer is max3(3, 1, 2)
var ab : integer is abs_int(-10)

print mn  // 1
print mx  // 3
print ab  // 10
