-- Campus ERP: authentication schema.
-- The app runs this automatically on first start; you can also run it by hand.
CREATE DATABASE IF NOT EXISTS university_auth;
USE university_auth;

CREATE TABLE IF NOT EXISTS users_auth (
    user_id         INT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    role            ENUM('STUDENT', 'INSTRUCTOR', 'ADMIN') NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    status          ENUM('ACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    full_name       VARCHAR(100),
    email           VARCHAR(120),
    last_login      DATETIME,
    failed_attempts INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
