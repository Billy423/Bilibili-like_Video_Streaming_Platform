-- Initialize Bilibili Database
-- This script runs when MySQL container starts for the first time

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the demo database
USE demo;

-- Create user if not exists
CREATE USER IF NOT EXISTS 'bilibili'@'%' IDENTIFIED BY 'bilibili123';
GRANT ALL PRIVILEGES ON demo.* TO 'bilibili'@'%';
FLUSH PRIVILEGES;

-- Create basic tables (you can add your actual schema here)
-- This is just a placeholder - replace with your actual table definitions

-- Example: User table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(50) UNIQUE,
    password VARCHAR(255),
    salt VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Example: User info table
CREATE TABLE IF NOT EXISTS t_user_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    nick VARCHAR(50),
    avatar VARCHAR(255),
    sign VARCHAR(255),
    gender VARCHAR(10),
    birth VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_nick (nick)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add more tables as needed based on your actual schema
