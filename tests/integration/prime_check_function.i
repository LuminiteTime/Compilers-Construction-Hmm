// Integration test: prime checking function

routine is_prime(n : integer) : boolean
is
  if n <= 1 then
    return false
  end
  var i : integer is 2
  while i * i <= n loop
    if n % i = 0 then
      return false
    end
    i := i + 1
  end
  return true
end

print is_prime(1)
print is_prime(2)
print is_prime(17)
print is_prime(18)
