// Integration test: Sieve of Eratosthenes for primes up to 20
type BoolArray is array [20] boolean

var isPrime : BoolArray

// Initialize all entries to true
for i in 1..20 loop
  isPrime[i] := true
end

// 1 is not prime
isPrime[1] := false

// Mark non-primes
for p in 2..20 loop
  if isPrime[p] then
    var m : integer is p * 2
    while m <= 20 loop
      isPrime[m] := false
      m := m + p
    end
  end
end

// Print all primes up to 20
for i in 1..20 loop
  if isPrime[i] then
    print i
  end
end
