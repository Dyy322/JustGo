package com.dyu.justgobackend.common.statemachine;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * 单条转换规则，描述从某个状态出发、匹配某事件后转换到目标状态。
 *
 * <p>包私有，仅由 {@link StateMachine.Builder} 创建。
 *
 * @param targetState 目标状态
 * @param guard       条件断言，{@code null} 表示无条件通过
 * @param <S>         状态枚举类型
 * @param <C>         上下文类型
 */
record TransitionRule<S extends Enum<S>, C>(S targetState, @Nullable Predicate<C> guard) {
}
