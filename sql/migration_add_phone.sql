-- ========================================
-- 数据库迁移脚本 - 添加 phone 字段
-- 适用于已部署的环境升级
-- ========================================

USE seat_reservation;

-- 添加 phone 字段（如果不存在）
ALTER TABLE `user` 
ADD COLUMN IF NOT EXISTS `phone` VARCHAR(16) DEFAULT '' COMMENT '手机号' AFTER `role`;

-- 验证
SELECT 'Migration completed! Added phone column to user table.' AS message;
DESCRIBE `user`;
