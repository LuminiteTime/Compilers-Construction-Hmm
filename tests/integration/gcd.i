// Integration test: Euclidean algorithm for GCD
routine gcd(a : integer, b : integer) : integer
is
  while b /= 0 loop
    var t : integer is b
    b := a % b
    a := t
  end
  return a
end

var g1 : integer is gcd(48, 18)
var g2 : integer is gcd(42, 28)

print g1  // 6
print g2  // 14
