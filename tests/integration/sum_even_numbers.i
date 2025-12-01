// Integration test: sum of even numbers from 1 to 20
var sum : integer is 0

for i in 1..20 loop
  if i % 2 = 0 then
    sum := sum + i
  end
end

// Expected: 2 + 4 + ... + 20 = 110
print sum
