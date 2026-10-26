-- Campus ERP: academic schema.
-- The app runs this automatically on first start; you can also run it by hand
-- after auth_db_create.sql. Demo data is loaded from the app's setup screen.
CREATE DATABASE IF NOT EXISTS university_erp;
USE university_erp;

CREATE TABLE IF NOT EXISTS students (
    user_id  INT PRIMARY KEY,
    roll_no  VARCHAR(20) NOT NULL UNIQUE,
    program  VARCHAR(100),
    year     INT,
    FOREIGN KEY (user_id) REFERENCES university_auth.users_auth(user_id)
);

CREATE TABLE IF NOT EXISTS instructors (
    user_id    INT PRIMARY KEY,
    department VARCHAR(100),
    office     VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES university_auth.users_auth(user_id)
);

CREATE TABLE IF NOT EXISTS courses (
    course_id   INT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    title       VARCHAR(200) NOT NULL,
    credits     INT NOT NULL,
    description TEXT,
    department  VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS sections (
    section_id     INT PRIMARY KEY AUTO_INCREMENT,
    course_id      INT NOT NULL,
    instructor_id  INT,
    day_time       VARCHAR(100),
    room           VARCHAR(50),
    capacity       INT NOT NULL,
    enrolled_count INT NOT NULL DEFAULT 0,
    semester       VARCHAR(20),
    year           INT,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (instructor_id) REFERENCES instructors(user_id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id    INT PRIMARY KEY AUTO_INCREMENT,
    student_id       INT NOT NULL,
    section_id       INT NOT NULL,
    status           ENUM('REGISTERED', 'DROPPED', 'COMPLETED') NOT NULL DEFAULT 'REGISTERED',
    enrollment_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    final_grade      VARCHAR(2),
    final_percentage DECIMAL(5,2),
    UNIQUE KEY unique_enrollment (student_id, section_id),
    FOREIGN KEY (student_id) REFERENCES students(user_id),
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);

-- Assessment components per section (e.g. Quiz 20 marks, 20% weight).
CREATE TABLE IF NOT EXISTS section_components (
    component_id INT PRIMARY KEY AUTO_INCREMENT,
    section_id   INT NOT NULL,
    name         VARCHAR(50) NOT NULL,
    max_score    DECIMAL(6,2) NOT NULL,
    weight       DECIMAL(5,2) NOT NULL,
    position     INT NOT NULL DEFAULT 0,
    UNIQUE KEY unique_component (section_id, name),
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);

CREATE TABLE IF NOT EXISTS grades (
    grade_id      INT PRIMARY KEY AUTO_INCREMENT,
    enrollment_id INT NOT NULL,
    component     VARCHAR(50) NOT NULL,
    score         DECIMAL(6,2),
    max_score     DECIMAL(6,2) DEFAULT 100,
    weight        DECIMAL(5,2) DEFAULT 0,
    UNIQUE KEY unique_grade (enrollment_id, component),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);

CREATE TABLE IF NOT EXISTS settings (
    setting_key   VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id INT PRIMARY KEY AUTO_INCREMENT,
    author_id       INT NOT NULL,
    section_id      INT NULL,
    title           VARCHAR(150) NOT NULL,
    body            TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    log_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    INT,
    username   VARCHAR(50),
    action     VARCHAR(60) NOT NULL,
    details    VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO settings (setting_key, setting_value) VALUES
    ('maintenance_mode', 'false'),
    ('institution_name', 'Campus University'),
    ('current_semester', 'Monsoon'),
    ('current_year', '2026'),
    ('add_deadline', '2026-12-31'),
    ('drop_deadline', '2026-12-31'),
    ('max_credits', '24'),
    ('lockout_threshold', '5');
