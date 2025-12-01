// Negative test: using reserved keyword as for-loop variable name
// Should fail at parse time with a clear error
for end in 1..3 loop
  print end
end
