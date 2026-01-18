USE university_erp;

-- Clear existing data
DELETE FROM grades;
DELETE FROM enrollments;
DELETE FROM sections;
DELETE FROM courses;
DELETE FROM students;
DELETE FROM instructors;
DELETE FROM settings;

-- Insert sample students
INSERT INTO students (user_id, roll_no, program, year) VALUES
(4, '2024100', 'Computer Science', 2024),
(5, '2025007', 'Electrical Engineering', 2025);

-- Insert sample instructors
INSERT INTO instructors (user_id, department, office) VALUES
(2, 'Computer Science', 'CSE'),
(3, 'Mathematics', 'MATH');

-- Insert sample courses
INSERT INTO courses (code, title, credits, description) VALUES
('CSE101', 'Introduction to Programming', 4, 'Basic programming concepts using Python'),
('ECE111', 'Digital Circuits', 4, 'Basic electrical circuit analysis'),
('COM101', 'Communication Skills', 4, 'Course about communication skills'),
('DES102', 'Introduction to HCI', 4, 'Basics of design, ui and hci'),
('CSE112', 'Computer Organisation', 4, 'Basics of co like assembler, simulator etc'),
('MTH203', 'Maths III', 4, 'Differential and integral calculus'),
('CSE231','Operating System', 4, 'Operating system in computers with concepts like memory virtualisation, scheduling policies etc' ),
('SSH201','Research Methods in Social Science and Design', 4, 'Course about quantitative and qualitative research' ),
('SG','Self Growth', 2, 'Self growth course in disciplines like chess, swimming etc.' ),
('CW', 'Community Work', 2, 'Mandatory community work in ngos or college summer camp');

-- Insert sample sections
INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) VALUES
(1, 2, 'Mon Wed 10:00-11:30', 'LHC-101', 300, 'Monsoon', 2025),
(1, 2, 'Tue Thu 10:00-11:30', 'LHC-102', 300, 'Monsoon', 2025),
(2, 3, 'Tue Thu 9:30-11:00', 'LHC-101', 300, 'Monsson', 2025),
(2, 3, 'Mon Wed 9:30-11:00', 'LHC-201', 290, 'Monsoon', 2025),
(7, 2, 'Mon Wed 15:00-16:30', 'Rnd-B003', 300, 'Monsoon', 2025),
(7, 2, 'Tue Thu 15:00-16:30', 'Rnd-B003', 310, 'Monsoon', 2025);


-- Insert sample enrollments
INSERT INTO enrollments (student_id, section_id, status) VALUES
(4, 1, 'REGISTERED'),
(4, 3, 'REGISTERED'),
(5, 2, 'REGISTERED'),
(5, 5, 'REGISTERED');

-- Insert sample grades
INSERT INTO grades (enrollment_id, component, score, max_score, weight) VALUES
(1, 'Quiz 1', 85, 100, 10),
(1, 'Midterm', 78, 100, 30),
(1, 'Final', 82, 100, 60),
(3, 'Quiz 1', 92, 100, 10),
(3, 'Midterm', 88, 100, 30),
(3, 'Midterm', 88, 100, 30);

-- Insert settings
INSERT INTO settings (setting_key, setting_value) VALUES
('maintenance_mode', 'false'),
('drop_deadline', '2025-12-15'),
('current_semester', 'Monsoon'),
('current_year', '2025');