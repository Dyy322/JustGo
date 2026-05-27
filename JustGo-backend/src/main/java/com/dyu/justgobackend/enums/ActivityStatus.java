package com.dyu.justgobackend.enums;

/**
 * 活动状态枚举，配合 {@code StateMachine} 管理状态流转。
 */
public enum ActivityStatus {

    RECRUITING(1, "招募中"),
    FULL(2, "已满员"),
    ONGOING(3, "进行中"),
    ENDED(4, "已结束"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String label;

    ActivityStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    /**
     * 根据 code 获取枚举值，code 无效时返回 {@code null}。
     */
    public static ActivityStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ActivityStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
