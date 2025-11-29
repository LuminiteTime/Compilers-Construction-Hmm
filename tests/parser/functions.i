routine add(a : integer, b : integer) : integer
is
  a + b
end

routine factorial(n : integer) : integer
is
  if n <= 1 then
    1
  else
    n * factorial(n - 1)
  end
end
