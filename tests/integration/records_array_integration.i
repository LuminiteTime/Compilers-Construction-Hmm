// Integration test: array of records and simple computations
type Point is record
  var x : integer is 0
  var y : integer is 0
end

var pts : array [3] Point

pts[1].x := 0
pts[1].y := 0
pts[2].x := 2
pts[2].y := 2
pts[3].x := 4
pts[3].y := 4

var sumX : integer is 0
var sumY : integer is 0

for i in 1..3 loop
  sumX := sumX + pts[i].x
  sumY := sumY + pts[i].y
end

var avgX : real is sumX / 3
var avgY : real is sumY / 3

print sumX  // 6
print sumY  // 6
print avgX
print avgY
