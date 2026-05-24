-- 关注 / 粉丝（社交关系）
-- 依赖：src/main/resources/SQL/user.sql 中的 `user` 表
-- 设计要点：
-- 1) user_follow 仅存边；uk_follower_followee 防重复、支撑 O(1) 判定是否关注
-- 2) 二级索引按「被查询用户 + 时间倒序 + 稳定 tie-break」支撑百万级游标分页，避免 OFFSET
-- 3) user_follow_stats 冗余计数，读写分离热点；可与离线任务对账 reconcile（此处不实现）
-- 4) DATETIME(3) 降低同一秒内大量写入时的排序抖动

USE JustGo;

CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `follower_id` BIGINT UNSIGNED NOT NULL COMMENT '关注方用户 ID',
    `followee_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注方用户 ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_followee` (`follower_id`, `followee_id`),
    KEY `idx_followee_created_follower` (`followee_id`, `created_at` DESC, `follower_id` DESC),
    KEY `idx_follower_created_followee` (`follower_id`, `created_at` DESC, `followee_id` DESC),
    CONSTRAINT `fk_user_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_user_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系';

CREATE TABLE IF NOT EXISTS `user_follow_stats` (
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `following_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '正在关注的人数',
    `follower_count` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '粉丝数',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_follow_stats_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注统计冗余表';

-- 扩容备忘（按需演进）：超大规模可对 user_follow 按 followee_id HASH 分区；读写分离下列表走只读副本；
-- stats 表可与离线任务按 COUNT 对账；极端热点可考虑异步队列合并写计数。
