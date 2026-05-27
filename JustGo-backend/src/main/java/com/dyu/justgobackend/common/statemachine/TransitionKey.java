package com.dyu.justgobackend.common.statemachine;

/**
 * (fromState, event) 复合键，作为状态机内部规则表的索引键。
 *
 * <p>包私有，不对外暴露。
 *
 * @param fromState 当前状态
 * @param event     触发事件
 * @param <S>       状态枚举类型
 * @param <E>       事件枚举类型
 */
record TransitionKey<S extends Enum<S>, E extends Enum<E>>(S fromState, E event) {
}
