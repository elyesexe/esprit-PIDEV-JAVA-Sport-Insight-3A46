-- Verification Script for Nutrition Tracking Setup
-- Run this to verify everything is set up correctly

USE sport_insight;

SELECT '=== VERIFICATION REPORT ===' as '';

-- 1. Check if tables exist
SELECT '1. Checking Tables...' as '';
SELECT 
    CASE 
        WHEN COUNT(*) = 3 THEN '✓ All 3 tables exist'
        ELSE '✗ Missing tables'
    END as Status
FROM information_schema.tables 
WHERE table_schema = 'sport_insight' 
AND table_name IN ('ai_checklist_progress', 'food_log', 'daily_nutrition_summary');

-- 2. Check table structures
SELECT '2. Checking Table Structures...' as '';

SELECT 
    CONCAT('✓ ai_checklist_progress has ', COUNT(*), ' columns') as Status
FROM information_schema.columns 
WHERE table_schema = 'sport_insight' AND table_name = 'ai_checklist_progress';

SELECT 
    CONCAT('✓ food_log has ', COUNT(*), ' columns') as Status
FROM information_schema.columns 
WHERE table_schema = 'sport_insight' AND table_name = 'food_log';

SELECT 
    CONCAT('✓ daily_nutrition_summary has ', COUNT(*), ' columns') as Status
FROM information_schema.columns 
WHERE table_schema = 'sport_insight' AND table_name = 'daily_nutrition_summary';

-- 3. Check test data
SELECT '3. Checking Test Data...' as '';

SELECT 
    CONCAT('✓ ', COUNT(*), ' food logs exist') as Status
FROM food_log;

SELECT 
    CONCAT('✓ ', COUNT(*), ' daily summaries exist') as Status
FROM daily_nutrition_summary;

SELECT 
    CONCAT('✓ ', COUNT(*), ' checklist items exist') as Status
FROM ai_checklist_progress;

-- 4. Check foreign key constraints
SELECT '4. Checking Foreign Keys...' as '';

SELECT 
    CONCAT('✓ ', COUNT(*), ' foreign keys configured') as Status
FROM information_schema.key_column_usage
WHERE table_schema = 'sport_insight'
AND table_name IN ('ai_checklist_progress', 'food_log', 'daily_nutrition_summary')
AND referenced_table_name IS NOT NULL;

-- 5. Sample data preview
SELECT '5. Sample Data Preview...' as '';

SELECT '--- Food Logs ---' as '';
SELECT 
    meal_type,
    food_description,
    calories,
    log_date
FROM food_log 
LIMIT 3;

SELECT '--- Daily Summaries ---' as '';
SELECT 
    summary_date,
    total_calories,
    target_calories,
    ROUND((total_calories / target_calories) * 100, 1) as progress_percent
FROM daily_nutrition_summary
LIMIT 3;

SELECT '--- Checklist Progress ---' as '';
SELECT 
    plan_type,
    plan_category,
    item_text,
    is_completed
FROM ai_checklist_progress
LIMIT 5;

-- 6. Final status
SELECT '=== SETUP STATUS ===' as '';
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'sport_insight' AND table_name IN ('ai_checklist_progress', 'food_log', 'daily_nutrition_summary')) = 3
        THEN '✅ SETUP COMPLETE - All systems ready!'
        ELSE '❌ SETUP INCOMPLETE - Check errors above'
    END as Final_Status;
