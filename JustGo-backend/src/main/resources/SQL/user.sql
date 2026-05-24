-- JustGo 用户表设计
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS JustGo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE JustGo;

-- 用户表
CREATE TABLE `user` (
    -- 主键
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',

    -- 基本信息
                        `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录用）',
                        `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
                        `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
                        `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
                        `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                        `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
                        `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
                        `birthday` DATE DEFAULT NULL COMMENT '出生日期',

    -- 账户状态
                        `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：0-禁用，1-正常，2-锁定，3-待激活',
                        `account_type` TINYINT NOT NULL DEFAULT 1 COMMENT '账户类型：1-普通用户，2-管理员，3-超级管理员',

    -- 安全相关
                        `salt` VARCHAR(50) DEFAULT NULL COMMENT '密码盐值',
                        `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
                        `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
                        `login_count` INT UNSIGNED DEFAULT 0 COMMENT '登录次数',
                        `failed_login_attempts` INT UNSIGNED DEFAULT 0 COMMENT '连续登录失败次数',
    -- 时间戳
                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
    -- 其他
                        `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                        `last_active_time` DATETIME DEFAULT NULL COMMENT '最后活跃时间',

    -- 索引
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`),
                        UNIQUE KEY `uk_email` (`email`),
                        UNIQUE KEY `uk_phone` (`phone`),
                        KEY `idx_status` (`status`),
                        KEY `idx_account_type` (`account_type`),
                        KEY `idx_created_at` (`created_at`),
                        KEY `idx_last_login_time` (`last_login_time`),
                        KEY `idx_deleted_at` (`deleted_at`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';