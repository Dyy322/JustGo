-- 活动模块 DDL
-- 依赖：src/main/resources/SQL/user.sql 中的 `user` 表
-- 设计要点：
-- 1) activity 存地理位置、时间、人数、状态，按分类/状态/时间索引
-- 2) activity_image 存 OSS objectKey，每活动最多 9 张
-- 3) activity_tag + activity_tag_rel 多对多，标签由管理员预设
-- 4) 人数上限用 0 表示不限，当前人数冗余计数避免 COUNT 查询
-- 5) 状态流转由 StateMachine 管理，定时任务兜底自动推进

USE JustGo;

-- 活动分类
CREATE TABLE IF NOT EXISTS activity_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(30) NOT NULL COMMENT '分类名称',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标标识（emoji/icon名称）',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序（越小越前）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动分类';

-- 活动标签
CREATE TABLE IF NOT EXISTS activity_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(30) NOT NULL COMMENT '标签名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动标签';

-- 活动
CREATE TABLE IF NOT EXISTS activity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    creator_id BIGINT UNSIGNED NOT NULL COMMENT '发起人用户ID',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    title VARCHAR(100) NOT NULL COMMENT '活动标题',
    description TEXT COMMENT '活动描述（富文本HTML）',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图URL（OSS objectKey）',
    location_name VARCHAR(200) NOT NULL COMMENT '地点名称（显示用）',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    address VARCHAR(300) DEFAULT NULL COMMENT '详细地址',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间（可空=无固定结束时间）',
    max_participants INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '人数上限（0=不限）',
    current_participants INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前报名人数（冗余计数）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=招募中 2=已满员 3=进行中 4=已结束 5=已取消',
    is_featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选',
    view_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
    PRIMARY KEY (id),
    KEY idx_category_status_time (category_id, status, start_time),
    KEY idx_creator (creator_id),
    KEY idx_start_time (start_time),
    KEY idx_featured (is_featured, start_time),
    KEY idx_deleted_at (deleted_at),
    CONSTRAINT fk_activity_creator FOREIGN KEY (creator_id) REFERENCES `user` (id),
    CONSTRAINT fk_activity_category FOREIGN KEY (category_id) REFERENCES activity_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动';

-- 活动图片
CREATE TABLE IF NOT EXISTS activity_image (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    activity_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    url VARCHAR(500) NOT NULL COMMENT '图片URL（OSS objectKey）',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_activity_sort (activity_id, sort_order),
    CONSTRAINT fk_image_activity FOREIGN KEY (activity_id) REFERENCES activity (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动图片';

-- 活动-标签关联
CREATE TABLE IF NOT EXISTS activity_tag_rel (
    activity_id BIGINT UNSIGNED NOT NULL COMMENT '活动ID',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    PRIMARY KEY (activity_id, tag_id),
    CONSTRAINT fk_tag_rel_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_tag_rel_tag FOREIGN KEY (tag_id) REFERENCES activity_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动-标签关联';

-- 种子数据：分类
INSERT IGNORE INTO activity_category (name, icon, sort_order) VALUES
('展览', '🎨', 1),
('运动', '⚽', 2),
('市集', '🎪', 3),
('音乐', '🎵', 4),
('美食', '🍜', 5),
('户外', '🏕️', 6),
('桌游', '🎲', 7),
('观影', '🎬', 8),
('学习', '📚', 9),
('其他', '✨', 99);

-- 种子数据：标签
INSERT IGNORE INTO activity_tag (name) VALUES
('周末'), ('免费'), ('亲子'), ('宠物友好'),
('新手友好'), ('摄影'), ('社交'), ('治愈'),
('高强度'), ('轻松'), ('室内'), ('室外'),
('限时'), ('长期'), ('线上');
