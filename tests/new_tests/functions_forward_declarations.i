// Test: Forward declarations
routine helper(x : integer) : integer  // forward declaration

routine main() : integer is
  return helper(10) + 5
end

// Full implementation after forward declaration
routine helper(x : integer) : integer is
  if x > 0 then
    return x * 2
  else
    return 0
  end
end

// Call the main function
var result is main()
print result  // Should be (10 * 2) + 5 = 25
