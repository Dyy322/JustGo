package com.dyu.justgobackend.common.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 泛型状态机引擎，管理领域对象的状态流转。
 *
 * <p>同键 {@code (fromState, event)} 下可定义多条转换规则，按
 * {@link Builder#add(Enum, Enum, Enum) add} 顺序评估：首个 Guard 通过的规则立即命中，
 * 无 Guard 的规则视为无条件通过（通常作为兜底规则放在最后）。
 *
 * <p>状态机只负责"校验 + 计算新状态"，不触碰数据库、不产生副作用，持久化由调用方负责。
 *
 * @param <S> 状态枚举类型
 * @param <E> 事件枚举类型
 * @param <C> 上下文类型（Guard 条件入参）；无需 Guard 的模块用 {@code Void}
 */
public class StateMachine<S extends Enum<S>, E extends Enum<E>, C> {

    private final Map<TransitionKey<S, E>, List<TransitionRule<S, C>>> rules;

    private StateMachine(Map<TransitionKey<S, E>, List<TransitionRule<S, C>>> rules) {
        this.rules = Collections.unmodifiableMap(rules);
    }

    /**
     * 执行状态转换。
     *
     * <p>查找所有匹配 {@code (from, event)} 的规则，按添加顺序评估 Guard：
     * 首个 Guard 为 {@code null} 或返回 {@code true} 的规则立即命中并返回目标状态。
     *
     * @param from    当前状态
     * @param event   触发事件
     * @param context 上下文对象，传给 Guard 的 {@link Predicate#test(Object)}，可为 {@code null}
     * @return 转换后的新状态
     * @throws IllegalTransitionException 当 {@code (from, event)} 无匹配规则，
     *                                    或所有 Guard 均拒绝时
     */
    public S fire(S from, E event, C context) {
        List<TransitionRule<S, C>> candidates = rules.get(new TransitionKey<>(from, event));
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalTransitionException(from, event);
        }

        for (TransitionRule<S, C> rule : candidates) {
            if (rule.guard() == null || rule.guard().test(context)) {
                return rule.targetState();
            }
        }

        throw new IllegalTransitionException(from, event, "Guard 条件不满足");
    }

    /**
     * 检查是否可以转换，不实际执行状态变更。
     *
     * @param from    当前状态
     * @param event   触发事件
     * @param context 上下文对象，可为 {@code null}
     * @return {@code true} 表示至少有一条规则匹配且 Guard 通过
     */
    public boolean canTransition(S from, E event, C context) {
        try {
            fire(from, event, context);
            return true;
        } catch (IllegalTransitionException e) {
            return false;
        }
    }

    /**
     * 获取指定状态下所有可能的转换。
     *
     * <p>同一事件对应多条规则时，仅返回第一条规则的 target（不评估 Guard）。
     * 主要用于 UI 展示当前状态下的可用操作按钮。
     *
     * @param from 当前状态
     * @return 事件到目标状态的不可变映射，无可用转换时返回空 Map
     */
    public Map<E, S> availableTransitions(S from) {
        Map<E, S> result = new LinkedHashMap<>();
        for (var entry : rules.entrySet()) {
            if (entry.getKey().fromState() == from) {
                E event = entry.getKey().event();
                result.putIfAbsent(event, entry.getValue().get(0).targetState());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * 创建 Builder 实例。
     *
     * @param <S> 状态枚举类型
     * @param <E> 事件枚举类型
     * @param <C> 上下文类型
     * @return 新的 Builder
     */
    public static <S extends Enum<S>, E extends Enum<E>, C> Builder<S, E, C> builder() {
        return new Builder<>();
    }

    /**
     * 状态机构建器，使用链式调用定义转换规则后调用 {@link #build()} 创建状态机实例。
     *
     * <p>同键 {@code (from, event)} 多条规则按 {@code add} 顺序评估，
     * 带 Guard 的规则应放在无 Guard 兜底规则之前。
     *
     * @param <S> 状态枚举类型
     * @param <E> 事件枚举类型
     * @param <C> 上下文类型
     */
    public static final class Builder<S extends Enum<S>, E extends Enum<E>, C> {

        private final Map<TransitionKey<S, E>, List<TransitionRule<S, C>>> rules = new LinkedHashMap<>();

        /**
         * 添加一条无条件转换规则（无 Guard，始终可执行）。
         *
         * @param from   当前状态
         * @param event  触发事件
         * @param target 目标状态
         * @return this Builder
         */
        public Builder<S, E, C> add(S from, E event, S target) {
            return addInternal(from, event, target, null);
        }

        /**
         * 添加一条带 Guard 的转换规则。
         *
         * @param from   当前状态
         * @param event  触发事件
         * @param target 目标状态
         * @param guard  条件断言，不可为 {@code null}；无 Guard 请使用 {@link #add(Enum, Enum, Enum)}
         * @return this Builder
         * @throws IllegalArgumentException 当 guard 为 {@code null}
         */
        public Builder<S, E, C> add(S from, E event, S target, Predicate<C> guard) {
            if (guard == null) {
                throw new IllegalArgumentException(
                        "guard 不能为 null，无 guard 请使用 add(from, event, target)");
            }
            return addInternal(from, event, target, guard);
        }

        /**
         * 构建状态机实例。
         *
         * @return 配置完成的 StateMachine
         */
        public StateMachine<S, E, C> build() {
            return new StateMachine<>(rules);
        }

        private Builder<S, E, C> addInternal(S from, E event, S target, Predicate<C> guard) {
            TransitionKey<S, E> key = new TransitionKey<>(from, event);
            rules.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new TransitionRule<>(target, guard));
            return this;
        }
    }
}
