-- ========================================
-- 增量迁移：清理旧的 AI 历史会话表
-- 背景：AI 助手已迁移到独立的 Python Agent 服务（ai-agent/），
--       会话记忆与历史改由 LangGraph SQLite checkpointer 承载，MySQL 两张 ai 表不再使用。
-- 全新安装无需执行本文件（init.sql 已不再创建这两张表）。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 seat_reservation < migration_drop_ai_history.sql
-- 注意：先删子表 ai_message（外键依赖 ai_conversation），再删 ai_conversation。
-- ========================================
SET NAMES utf8mb4;

DROP TABLE IF EXISTS `ai_message`;
DROP TABLE IF EXISTS `ai_conversation`;
