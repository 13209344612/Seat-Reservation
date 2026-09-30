-- ========================================
-- Seat Reservation System - Migration V2
-- 核心业务缺陷修复：库存模型改为派生计数、唯一索引支持取消后重约
--
-- 适用对象：已按旧版 init.sql 建好、含数据的存量库。
-- 全新安装请直接执行 init.sql，无需本脚本。
-- 执行前务必备份数据库。
-- ========================================

SET NAMES utf8mb4;
USE seat_reservation;

-- ----------------------------------------
-- 1. study_room：容量改为派生计数，删除房间级计数器与乐观锁版本号
-- ----------------------------------------
ALTER TABLE `study_room`
    DROP COLUMN `available_capacity`,
    DROP COLUMN `version`;

-- ----------------------------------------
-- 2. reservation：唯一索引改造，支持「取消/过期后可重约」，同时禁止重复活跃预约
--    用生成列 active_unique：仅 booked/signed 生成去重键，其余置 NULL，
--    MySQL 唯一索引允许多个 NULL，故历史取消/过期记录不冲突。
-- ----------------------------------------
ALTER TABLE `reservation`
    DROP INDEX `uk_user_room_slot_date`;

ALTER TABLE `reservation`
    ADD COLUMN `active_unique` VARCHAR(128) GENERATED ALWAYS AS (
        IF(`status` IN ('booked','signed'),
           CONCAT_WS('#', `user_id`, `room_id`, `time_slot_id`, `reservation_date`), NULL)
    ) VIRTUAL COMMENT '活跃预约去重键（VIRTUAL；勿改STORED，会与基列的ON DELETE CASCADE外键冲突）';

ALTER TABLE `reservation`
    ADD UNIQUE KEY `uk_active_reservation` (`active_unique`);

-- ----------------------------------------
-- 3. 校验
-- ----------------------------------------
SELECT 'Migration V2 completed!' AS message;
SELECT COUNT(*) AS reservation_count FROM `reservation`;
