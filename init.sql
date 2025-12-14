-- 创建数据库
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS health_manage_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE health_manage_db;

-- ==========================================
-- 1. 表结构初始化
-- ==========================================

-- 1.1 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '用户表';


-- 1.2 健康数据表
CREATE TABLE IF NOT EXISTS health_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    weight DECIMAL(5, 2) COMMENT '体重(kg)',
    heart_rate INT COMMENT '心率(bpm)',
    systolic INT COMMENT '收缩压(mmHg)',
    diastolic INT COMMENT '舒张压(mmHg)',
    steps INT COMMENT '步数',
    blood_sugar DECIMAL(4, 1) COMMENT '血糖(mmol/L)',
    record_date DATE NOT NULL COMMENT '记录日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    alarm_status TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_date (user_id, record_date)
) COMMENT '健康数据表';

SET @col_exists := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'health_data' AND COLUMN_NAME = 'alarm_status'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE health_data ADD COLUMN alarm_status TINYINT NOT NULL DEFAULT 0;',
  'SELECT 1;'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 打卡任务表
CREATE TABLE IF NOT EXISTS check_in_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    task_name VARCHAR(50) NOT NULL COMMENT '任务名称',
    icon VARCHAR(50) COMMENT '图标名称',
    color VARCHAR(20) COMMENT '颜色代码',
    target_desc VARCHAR(50) COMMENT '目标描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '打卡任务表';

-- 1.4 打卡记录表
CREATE TABLE IF NOT EXISTS check_in_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    record_date DATE NOT NULL COMMENT '打卡日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_date (task_id, record_date)
) COMMENT '打卡记录表';

-- 1.5 饮食记录表
CREATE TABLE IF NOT EXISTS diet_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL COMMENT 'breakfast, lunch, dinner, snack',
    food_name VARCHAR(100) NOT NULL COMMENT '食物名称',
    calories INT NOT NULL COMMENT '卡路里',
    amount VARCHAR(50) COMMENT '数量描述',
    record_date DATE NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '饮食记录表';

-- ==========================================
-- 2. 数据初始化
-- ==========================================

-- 2.1 插入/更新内置用户
-- 将原 admin 作为普通用户保留（ID=1），新增管理员 admin2（ID=2）
INSERT INTO sys_user (id, username, password, nickname, role) VALUES (1, 'user', '123456', '韩五', 'USER')
ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), role=VALUES(role);

INSERT INTO sys_user (id, username, password, nickname, role) VALUES (2, 'admin2', '123456', '管理员', 'ADMIN')
ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), role=VALUES(role);

-- 2.2 插入健康数据 (admin, id=1)
INSERT INTO health_data (user_id, weight, heart_rate, systolic, diastolic, steps, blood_sugar, record_date) VALUES
(1, 66.5, 78, 125, 82, 4500, 5.6, DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
(1, 66.2, 75, 122, 80, 6200, 5.5, DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
(1, 66.0, 72, 120, 79, 8000, 5.4, DATE_SUB(CURDATE(), INTERVAL 4 DAY)),
(1, 64.8, 70, 119, 78, 10500, 5.3, DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
(1, 65.5, 71, 118, 77, 9800, 5.3, DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(1, 64.2, 69, 118, 76, 12000, 5.2, DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
(1, 65.0, 68, 116, 75, 5600, 5.1, CURDATE())
ON DUPLICATE KEY UPDATE weight=VALUES(weight), steps=VALUES(steps);

INSERT INTO health_data (user_id, weight, heart_rate, systolic, diastolic, steps, blood_sugar, record_date, alarm_status) VALUES
(2, 70.0, 105, 150, 95, 3000, 8.2, CURDATE(), 1)
ON DUPLICATE KEY UPDATE weight=VALUES(weight), heart_rate=VALUES(heart_rate), systolic=VALUES(systolic), diastolic=VALUES(diastolic), steps=VALUES(steps), blood_sugar=VALUES(blood_sugar), alarm_status=VALUES(alarm_status);

-- 2.3 插入打卡任务 (admin, id=1)
-- 先清空旧任务
DELETE FROM check_in_task WHERE user_id = 1;

INSERT INTO check_in_task (user_id, task_name, icon, color, target_desc) VALUES
(1, '晨跑', 'Bicycle', '#409EFF', '目标：3公里'),
(1, '多喝水', 'GobletFull', '#67C23A', '目标：2000ml'),
(1, '早睡', 'Moon', '#909399', '23:00 前睡觉'),
(1, '吃水果', 'Apple', '#F56C6C', '每天一个苹果');

CREATE TABLE IF NOT EXISTS check_in_task_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(20),
    target_desc VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '系统默认打卡任务模板';

CREATE TABLE IF NOT EXISTS reminder_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    content VARCHAR(200) NOT NULL,
    trigger_time VARCHAR(20),
    enabled TINYINT(1) DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '提醒模板';

INSERT INTO check_in_task_template (task_name, icon, color, target_desc)
SELECT '晨跑','Bicycle','#409EFF','目标：3公里' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM check_in_task_template WHERE task_name='晨跑');
INSERT INTO check_in_task_template (task_name, icon, color, target_desc)
SELECT '多喝水','GobletFull','#67C23A','目标：2000ml' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM check_in_task_template WHERE task_name='多喝水');
INSERT INTO check_in_task_template (task_name, icon, color, target_desc)
SELECT '早睡','Moon','#909399','23:00 前睡觉' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM check_in_task_template WHERE task_name='早睡');
INSERT INTO check_in_task_template (task_name, icon, color, target_desc)
SELECT '吃水果','Apple','#F56C6C','每天一个苹果' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM check_in_task_template WHERE task_name='吃水果');

INSERT INTO reminder_template (name, content, trigger_time, enabled)
SELECT '多喝水提醒','今天记得多喝水','09:00',1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reminder_template WHERE name='多喝水提醒');
INSERT INTO reminder_template (name, content, trigger_time, enabled)
SELECT '早睡提醒','23:00 前睡觉','22:30',1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM reminder_template WHERE name='早睡提醒');

-- 2.4 插入饮食记录 (admin, id=1)
DELETE FROM diet_record WHERE user_id = 1 AND record_date = CURDATE();

INSERT INTO diet_record (user_id, meal_type, food_name, calories, amount, record_date) VALUES
(1, 'breakfast', '牛奶', 135, '1盒', CURDATE()),
(1, 'breakfast', '全麦面包', 150, '2片', CURDATE()),
(1, 'lunch', '米饭', 174, '1碗', CURDATE()),
(1, 'lunch', '香煎鸡胸肉', 220, '150g', CURDATE()),
(1, 'lunch', '西兰花', 50, '1份', CURDATE()),
(1, 'dinner', '蔬菜沙拉', 120, '1碗', CURDATE()),
(1, 'snack', '酸奶', 120, '1杯', CURDATE());
