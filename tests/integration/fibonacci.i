routine fibonacci(n : integer) : integer
is
  if n <= 1 then
    n
  else
    fibonacci(n - 1) + fibonacci(n - 2)
  end
end

var fib10 is fibonacci(10)
print fib10
