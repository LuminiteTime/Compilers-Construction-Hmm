// Integration test: Collatz sequence step count

routine collatz_steps(n : integer) : integer
is
  var steps : integer is 0
  var x : integer is n
  while x /= 1 loop
    if x % 2 = 0 then
      x := x / 2
    else
      x := 3 * x + 1
    end
    steps := steps + 1
  end
  return steps
end

var c1 : integer is collatz_steps(6)
var c2 : integer is collatz_steps(11)

print c1
print c2
