routine add(a : integer, b : integer) : integer
  is
    a + b
  end

routine main() is
  var result : integer is add(10, 20)
  print result
end

