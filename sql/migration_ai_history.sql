-- ========================================
-- 增量迁移：AI 历史会话表（用于已初始化的既有数据库）
-- 全新安装无需执行本文件，init.sql 已包含这两张表。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 seat_reservation < migration_ai_history.sql
-- ========================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID（与ChatMemory/前端一致）',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `title` VARCHAR(128) NOT NULL DEFAULT '新对话' COMMENT '会话标题（取首条提问）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    UNIQUE KEY uk_conversation_id (`conversation_id`),
    INDEX idx_user_update (`user_id`, `update_time`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话表';

CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(16) NOT NULL COMMENT '角色：user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation (`conversation_id`, `id`),
    FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation`(`conversation_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息表';
