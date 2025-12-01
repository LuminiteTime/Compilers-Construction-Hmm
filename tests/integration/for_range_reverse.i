// Integration test: for-range loops with and without reverse

var s_forward : integer is 0
for i in 1..5 loop
  s_forward := s_forward * 10 + i
end

var s_reverse : integer is 0
for i in 1..5 reverse loop
  s_reverse := s_reverse * 10 + i
end

// Expected numeric output (ignoring spaces/newlines): 1234554321
print s_forward  // 12345
print s_reverse  // 54321
