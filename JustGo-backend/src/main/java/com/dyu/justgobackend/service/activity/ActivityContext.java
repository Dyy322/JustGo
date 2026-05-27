package com.dyu.justgobackend.service.activity;

/**
 * 活动状态机 Guard 上下文，封装运行时数据供 Guard 条件判断。
 */
public record ActivityContext(
        int currentParticipants,
        int maxParticipants,
        boolean isCreator
) {
    public boolean isFull() {
        return maxParticipants > 0 && currentParticipants >= maxParticipants;
    }
}
