routine factorial(n : integer) : integer
is
  if n <= 1 then
    return 1
  else
    return n * factorial(n - 1)
  end
end

var result : integer is factorial(5)
print result
