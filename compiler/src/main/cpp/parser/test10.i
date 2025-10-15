type Student is record
    var id: integer;
    var grade: real;
end
var students: array[3] Student;
students[1].id := 101;
students[1].grade := 85.5;
for student in students loop
    print student.id, student.grade;
end
