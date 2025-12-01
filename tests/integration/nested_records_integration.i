// Integration test: nested records and record fields passed to routines
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

routine give_raise(p : Person) : integer
is
  p.job.salary := p.job.salary + 100
  return p.job.salary
end

var john : Person
john.age := 30
john.home.street := 10
john.home.house := 5
john.work.street := 20
john.work.house := 7
john.job.salary := 1000
john.job.details.level := 3
john.job.details.hours := 40

var new_salary : integer is give_raise(john)

print john.age
print john.home.street
print john.home.house
print john.work.street
print john.work.house
print john.job.salary
print new_salary
print john.job.details.level
print john.job.details.hours
