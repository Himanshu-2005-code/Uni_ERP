CREATE DATABASE IF NOT EXISTS university_erp;
USE university_erp;

CREATE TABLE students (
    user_id INT PRIMARY KEY,
    roll_no VARCHAR(20) UNIQUE NOT NULL,
    program VARCHAR(100),
    year INT,
    FOREIGN KEY (user_id) REFERENCES university_auth.users_auth(user_id)
);

CREATE TABLE instructors (
    user_id INT PRIMARY KEY,
    department VARCHAR(100),
    office VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES university_auth.users_auth(user_id)
);

CREATE TABLE courses (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    credits INT NOT NULL,
    description TEXT
);

CREATE TABLE sections (
    section_id INT PRIMARY KEY AUTO_INCREMENT,
    course_id INT NOT NULL,
    instructor_id INT,
    day_time VARCHAR(100),
    room VARCHAR(50),
    capacity INT NOT NULL,
    enrolled_count INT DEFAULT 0,
    semester VARCHAR(20),
    year INT,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (instructor_id) REFERENCES instructors(user_id)
);

CREATE TABLE enrollments (
    enrollment_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    section_id INT NOT NULL,
    status ENUM('REGISTERED', 'DROPPED', 'COMPLETED') DEFAULT 'REGISTERED',
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    final_grade VARCHAR(2),
    UNIQUE KEY unique_enrollment (student_id, section_id),
    FOREIGN KEY (student_id) REFERENCES students(user_id),
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);

CREATE TABLE grades (
    grade_id INT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id INT NOT NULL,
    component VARCHAR(50) NOT NULL,
    score DECIMAL(5,2),
    max_score DECIMAL(5,2) DEFAULT 100,
    weight DECIMAL(5,2) DEFAULT 0,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);

CREATE TABLE settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255)
);

-- Create indexes for performance
CREATE INDEX idx_sections_course ON sections(course_id);
CREATE INDEX idx_sections_instructor ON sections(instructor_id);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_section ON enrollments(section_id);
CREATE INDEX idx_grades_enrollment ON grades(enrollment_id);