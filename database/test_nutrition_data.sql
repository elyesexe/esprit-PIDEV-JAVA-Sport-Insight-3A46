-- Test data for nutrition tracking
USE sport_insight;

-- Insert test food logs (assuming user_id 1 exists)
INSERT INTO food_log (user_id, log_date, meal_type, food_description, calories, protein_g, carbs_g, fat_g, fiber_g, api_response)
VALUES 
(1, CURDATE(), 'breakfast', '2 oeufs, 2 tranches pain complet, 1 banane', 450.0, 20.0, 65.0, 12.0, 8.0, '{"mock": true}'),
(1, CURDATE(), 'lunch', '200g poulet grillé, 1 tasse riz, légumes', 550.0, 45.0, 60.0, 8.0, 5.0, '{"mock": true}'),
(1, CURDATE(), 'snack', '1 pomme, 30g amandes', 250.0, 6.0, 30.0, 15.0, 7.0, '{"mock": true}');

-- Insert daily summary
INSERT INTO daily_nutrition_summary (user_id, summary_date, total_calories, total_protein_g, total_carbs_g, total_fat_g, total_fiber_g, target_calories)
VALUES 
(1, CURDATE(), 1250.0, 71.0, 155.0, 35.0, 20.0, 2500.0)
ON DUPLICATE KEY UPDATE 
    total_calories = 1250.0,
    total_protein_g = 71.0,
    total_carbs_g = 155.0,
    total_fat_g = 35.0,
    total_fiber_g = 20.0;

-- Insert test checklist progress
INSERT INTO ai_checklist_progress (user_id, plan_type, plan_category, item_text, is_completed, completed_at)
VALUES 
(1, 'exercise', 'cardio', 'Course continue 30 min (70% FCmax)', true, NOW()),
(1, 'exercise', 'cardio', 'HIIT 20 min (30s sprint / 90s repos)', false, NULL),
(1, 'meal', 'breakfast', '80g flocons d\'avoine', true, NOW()),
(1, 'meal', 'breakfast', '250ml lait demi-écrémé', true, NOW());

SELECT 'Test data inserted successfully!' as Status;

-- Verify data
SELECT COUNT(*) as food_logs FROM food_log WHERE user_id = 1 AND log_date = CURDATE();
SELECT COUNT(*) as summaries FROM daily_nutrition_summary WHERE user_id = 1 AND summary_date = CURDATE();
SELECT COUNT(*) as checklist_items FROM ai_checklist_progress WHERE user_id = 1;
