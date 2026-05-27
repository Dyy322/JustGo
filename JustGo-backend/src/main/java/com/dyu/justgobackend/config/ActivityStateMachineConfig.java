package com.dyu.justgobackend.config;

import com.dyu.justgobackend.common.statemachine.StateMachine;
import com.dyu.justgobackend.enums.ActivityEvent;
import com.dyu.justgobackend.enums.ActivityStatus;
import com.dyu.justgobackend.service.activity.ActivityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.dyu.justgobackend.enums.ActivityEvent.*;
import static com.dyu.justgobackend.enums.ActivityStatus.*;

@Configuration
public class ActivityStateMachineConfig {

    @Bean
    public StateMachine<ActivityStatus, ActivityEvent, ActivityContext> activityStateMachine() {
        return StateMachine.<ActivityStatus, ActivityEvent, ActivityContext>builder()
                // JOIN: 满员→FULL，未满→保持 RECRUITING
                .add(RECRUITING, JOIN, FULL, ctx -> ctx.isFull())
                .add(RECRUITING, JOIN, RECRUITING)
                // JOIN: 满员后有人取消 → 回到 RECRUITING
                .add(FULL, JOIN, RECRUITING, ctx -> !ctx.isFull())

                // CANCEL: 招募中 / 已满员 / 进行中均可取消
                .add(RECRUITING, CANCEL, CANCELLED)
                .add(FULL, CANCEL, CANCELLED)
                .add(ONGOING, CANCEL, CANCELLED)

                // START: 活动开始
                .add(RECRUITING, START, ONGOING)
                .add(FULL, START, ONGOING)

                // END: 活动结束
                .add(ONGOING, END, ENDED)

                .build();
    }
}
