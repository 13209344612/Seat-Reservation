-- ========================================
-- Seat Reservation System - Database Initialization
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS seat_reservation DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seat_reservation;

-- ========================================
-- 1. 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `role` VARCHAR(16) NOT NULL DEFAULT 'student' COMMENT '角色：student/admin',
    `phone` VARCHAR(16) DEFAULT '' COMMENT '手机号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_username (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 2. 自习室表
-- ========================================
CREATE TABLE IF NOT EXISTS `study_room` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自习室ID',
    `name` VARCHAR(64) NOT NULL COMMENT '自习室名称',
    `total_capacity` INT NOT NULL COMMENT '总容量',
    `available_capacity` INT NOT NULL COMMENT '可用容量',
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-正常，1-维护',
    `location` VARCHAR(128) COMMENT '位置信息',
    `description` VARCHAR(500) COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室表';

-- ========================================
-- 3. 时段表
-- ========================================
CREATE TABLE IF NOT EXISTS `time_slot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '时段ID',
    `room_id` BIGINT NOT NULL COMMENT '自习室ID',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-启用，1-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`room_id`) REFERENCES `study_room`(`id`) ON DELETE CASCADE,
    INDEX idx_room_id (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时段表';

-- ========================================
-- 4. 预约记录表
-- ========================================
CREATE TABLE IF NOT EXISTS `reservation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `room_id` BIGINT NOT NULL COMMENT '自习室ID',
    `time_slot_id` BIGINT NOT NULL COMMENT '时段ID',
    `reservation_date` DATE NOT NULL COMMENT '预约日期',
    `status` VARCHAR(16) DEFAULT 'booked' COMMENT '状态：booked-已预约，signed-已签到，cancelled-已取消，expired-已过期',
    `sign_time` DATETIME COMMENT '签到时间',
    `cancel_time` DATETIME COMMENT '取消时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`room_id`) REFERENCES `study_room`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`time_slot_id`) REFERENCES `time_slot`(`id`) ON DELETE CASCADE,
    UNIQUE KEY uk_user_room_slot_date (`user_id`, `room_id`, `time_slot_id`, `reservation_date`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_room_id (`room_id`),
    INDEX idx_reservation_date (`reservation_date`),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录表';

-- ========================================
-- 插入测试数据
-- ========================================

-- 插入自习室数据
INSERT INTO `study_room` (`id`, `name`, `total_capacity`, `available_capacity`, `location`, `description`) VALUES
(1, '自习室A（1号馆）', 50, 50, '图书馆1楼', '安静舒适，适合深度学习'),
(2, '自习室B（2号馆）', 30, 30, '图书馆2楼', '采光良好，视野开阔'),
(3, '自习室C（图书馆）', 80, 80, '图书馆3楼', '空间宽敞，设施齐全');

-- 插入时段数据
INSERT INTO `time_slot` (`room_id`, `start_time`, `end_time`) VALUES
(1, '08:00:00', '12:00:00'),
(1, '13:00:00', '17:00:00'),
(1, '18:00:00', '22:00:00'),
(2, '08:00:00', '12:00:00'),
(2, '13:00:00', '17:00:00'),
(2, '18:00:00', '22:00:00'),
(3, '08:00:00', '12:00:00'),
(3, '13:00:00', '17:00:00'),
(3, '18:00:00', '22:00:00');

-- 插入测试用户（密码都是 123456，BCrypt 加密后的值）
INSERT INTO `user` (`username`, `password`, `role`) VALUES
('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'student'),
('student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'student'),
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin');

-- ========================================
-- 验证数据
-- ========================================
SELECT 'Database initialization completed!' AS message;
SELECT COUNT(*) AS user_count FROM `user`;
SELECT COUNT(*) AS room_count FROM `study_room`;
SELECT COUNT(*) AS timeslot_count FROM `time_slot`;
