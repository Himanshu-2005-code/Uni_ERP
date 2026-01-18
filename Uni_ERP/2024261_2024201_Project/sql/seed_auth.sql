USE university_auth;

-- Clear existing data
DELETE FROM users_auth;

-- Insert sample users (passwords are hashed versions of: password123)
INSERT INTO users_auth (username, role, password_hash) VALUES
('admin1', 'ADMIN', '$2a$10$I4QKlIn6DzY3AvmWdkmkcuZ0Y5IC9DrMubAQkdfqvm5eJ9PlEvhwO'), 
('inst1', 'INSTRUCTOR', '$2a$10$b5t4sATKbcSB6dzIi03ooO9hUMtCEaSZu1ADAVgrCvj.2yyNec2Fi'),
('inst2', 'INSTRUCTOR', '$2a$10$wI2pFTYHehm5fT9Vzj4TTOq3EHBp51ChzDa1ibF08EnGP8Dd6RKPC'),
('stu1', 'STUDENT', '$2a$10$IFACpTpmVAZ8h1qO9rk5P.8pDCgQUx7GAC44hjyfYQ2png5H/zGva'),
('stu2', 'STUDENT', '$2a$10$5JKuf5oBOhb6D85LcsOuO.G3OYVM9mxpHI9kTi6UnLJo/ZZer8z4e');
