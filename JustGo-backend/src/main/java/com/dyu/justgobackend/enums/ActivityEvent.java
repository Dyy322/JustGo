package com.dyu.justgobackend.enums;

/**
 * 活动事件枚举，作为 {@code StateMachine} 的触发事件。
 */
public enum ActivityEvent {
    JOIN,
    CANCEL,
    START,
    END,
    REPUBLISH
}
