// Big integration test: complex program using most language features

type Address is record
  var street : integer is 0
  var house  : integer is 0
end

type JobDetails is record
  var level : integer is 0
  var hours : integer is 0
end

type Job is record
  var salary : integer is 0
  var details : JobDetails
end

type Person is record
  var age  : integer is 0
  var home : Address
  var work : Address
  var job  : Job
end

type IntArray5 is array [5] integer
type Matrix3x3 is array [3] array [3] integer

// Sum elements of a fixed-size array, used from main program
routine sum_array(a : IntArray5, n : integer) : integer
is
  var s : integer is 0
  var i : integer is 1
  while i <= n loop
    s := s + a[i]
    i := i + 1
  end
  return s
end

routine max_array(a : IntArray5, n : integer) : integer
is
  var i : integer is 1
  var mx : integer is a[1]
  i := 2
  while i <= n loop
    if a[i] > mx then
      mx := a[i]
    end
    i := i + 1
  end
  return mx
end

// Work with a nested record type Matrix3x3
routine trace_matrix_3x3(M : Matrix3x3) : integer
is
  var t : integer is 0
  t := t + M[1][1]
  t := t + M[2][2]
  t := t + M[3][3]
  return t
end

// Routines that operate on nested record fields
routine set_address(addr : Address, street : integer, house : integer)
is
  addr.street := street
  addr.house := house
end

routine setup_job(p : Person, salary : integer, level : integer, hours : integer)
is
  p.job.salary := salary
  p.job.details.level := level
  p.job.details.hours := hours
end

// Adjust salary and nested job details of a person
routine normalize_salary(p : Person, bonus : integer) : integer
is
  p.job.salary := p.job.salary + bonus
  p.job.details.hours := p.job.details.hours + 5
  return p.job.salary
end

// Initialize an IntArray5 with values 1..5 via a loop
routine init_array_1_to_5(a : IntArray5)
is
  var i : integer is 1
  while i <= 5 loop
    a[i] := i
    i := i + 1
  end
end

// Main program

var john : Person
john.age := 30
set_address(john.home, 10, 5)
set_address(john.work, 20, 7)
setup_job(john, 1000, 2, 40)

var jane : Person
jane.age := 25
set_address(jane.home, 11, 9)
set_address(jane.work, 21, 8)
setup_job(jane, 800, 1, 35)

var john_new_salary : integer is normalize_salary(john, 150)

var nums : IntArray5
init_array_1_to_5(nums)

var sumNums : integer is sum_array(nums, 5)
var maxNums : integer is max_array(nums, 5)
var avgNums : real is sumNums / 5

var mat : Matrix3x3

// Fill mat[i][j] = i*10 + j
var i : integer is 1
while i <= 3 loop
  var j : integer is 1
  while j <= 3 loop
    mat[i][j] := i * 10 + j
    j := j + 1
  end
  i := i + 1
end

var trace : integer is trace_matrix_3x3(mat)

// Compute gcd of 48 and 18 using while loop
var a : integer is 48
var b : integer is 18
while b /= 0 loop
  var tmp : integer is b
  b := a % b
  a := tmp
end
var gcd_ab : integer is a

var cond : boolean is (john.age > jane.age) and (maxNums >= 5)

// Print results
print john.age
print john.home.street
print john.home.house
print john.work.street
print john.work.house
print john.job.salary
print john_new_salary
print jane.age
print jane.job.salary
print sumNums
print maxNums
print avgNums
print trace
print gcd_ab
print cond
