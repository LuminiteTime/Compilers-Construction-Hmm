routine fibonacci(n : integer) : integer
is
  if n <= 1 then
    return n
  else
    return fibonacci(n - 1) + fibonacci(n - 2)
  end
end

var fib10 : integer is fibonacci(10)
// Result is tested by successful execution
