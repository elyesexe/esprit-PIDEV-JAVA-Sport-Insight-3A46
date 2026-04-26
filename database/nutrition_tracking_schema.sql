-- Nutrition Tracking Tables for Sport Insight
-- Add these tables to your existing database

USE `sport_insight`;

-- Table to save AI recommendation checklist progress
CREATE TABLE IF NOT EXISTS `ai_checklist_progress` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `plan_type` VARCHAR(50) NOT NULL COMMENT 'exercise or meal',
    `plan_category` VARCHAR(50) NOT NULL COMMENT 'cardio, strength, breakfast, lunch, etc.',
    `item_text` VARCHAR(500) NOT NULL,
    `is_completed` BOOLEAN NOT NULL DEFAULT FALSE,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_checklist_user_id` (`user_id`),
    KEY `idx_checklist_plan_type` (`plan_type`),
    KEY `idx_checklist_plan_category` (`plan_category`),
    CONSTRAINT `fk_checklist_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table to track daily food intake
CREATE TABLE IF NOT EXISTS `food_log` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `log_date` DATE NOT NULL,
    `meal_type` VARCHAR(50) NOT NULL COMMENT 'breakfast, lunch, dinner, snack',
    `food_description` TEXT NOT NULL COMMENT 'What the user ate',
    `calories` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `protein_g` DECIMAL(8,2) NULL,
    `carbs_g` DECIMAL(8,2) NULL,
    `fat_g` DECIMAL(8,2) NULL,
    `fiber_g` DECIMAL(8,2) NULL,
    `api_response` TEXT NULL COMMENT 'Store full API response for reference',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_food_log_user_id` (`user_id`),
    KEY `idx_food_log_date` (`log_date`),
    KEY `idx_food_log_meal_type` (`meal_type`),
    CONSTRAINT `fk_food_log_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table to track daily nutrition summary
CREATE TABLE IF NOT EXISTS `daily_nutrition_summary` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT NOT NULL,
    `summary_date` DATE NOT NULL,
    `total_calories` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_protein_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_carbs_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_fat_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `total_fiber_g` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `target_calories` DECIMAL(8,2) NULL COMMENT 'Daily calorie goal',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_date` (`user_id`, `summary_date`),
    KEY `idx_summary_user_id` (`user_id`),
    KEY `idx_summary_date` (`summary_date`),
    CONSTRAINT `fk_summary_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
