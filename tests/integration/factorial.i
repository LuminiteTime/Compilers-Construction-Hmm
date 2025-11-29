routine factorial(n : integer) : integer
is
  if n <= 1 then
    1
  else
    n * factorial(n - 1)
  end
end

var result is factorial(5)
print result
