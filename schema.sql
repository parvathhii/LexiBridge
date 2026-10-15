-- ==============================================================================
-- LexiBridge – Dyslexia-Friendly Learning Portal
-- MySQL Database Schema
-- Database Name: lexibridge
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `lexibridge`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `lexibridge`;

-- Table: users
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `role` ENUM('ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER', 'ROLE_ADMIN') NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email` (`email`),
    INDEX `idx_user_username` (`username`),
    INDEX `idx_user_email` (`email`),
    INDEX `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
