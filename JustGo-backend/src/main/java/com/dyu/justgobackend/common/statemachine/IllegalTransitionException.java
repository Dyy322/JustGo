package com.dyu.justgobackend.common.statemachine;

import com.dyu.justgobackend.exception.BusinessException;

/**
 * 状态机禁止的转换发生时抛出。
 *
 * <p>继承 {@link BusinessException}，由 {@code GlobalExceptionHandler} 统一转为
 * HTTP 400 响应，错误消息中文、可直接展示给用户。
 */
public class IllegalTransitionException extends BusinessException {

    /**
     * @param from  当前状态
     * @param event 触发事件
     * @param <S>   状态枚举类型
     * @param <E>   事件枚举类型
     */
    public <S extends Enum<S>, E extends Enum<E>> IllegalTransitionException(S from, E event) {
        super(400, String.format("不允许的状态转换: [%s] --%s--> ?", from, event));
    }

    /**
     * @param from   当前状态
     * @param event  触发事件
     * @param reason 附加原因（如 "Guard 条件不满足"），会拼入错误消息
     * @param <S>    状态枚举类型
     * @param <E>    事件枚举类型
     */
    public <S extends Enum<S>, E extends Enum<E>> IllegalTransitionException(
            S from, E event, String reason) {
        super(400, String.format("不允许的状态转换: [%s] --%s--> ? 原因: %s", from, event, reason));
    }
}
