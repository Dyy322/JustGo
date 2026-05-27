package com.dyu.justgobackend.scheduler;

import com.dyu.justgobackend.enums.ActivityStatus;
import com.dyu.justgobackend.mapper.activity.ActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动状态自动流转定时任务，每 5 分钟批量推进活动状态。
 */
@Component
public class ActivityStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivityStatusScheduler.class);

    private final ActivityMapper activityMapper;

    public ActivityStatusScheduler(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void advanceStatus() {
        LocalDateTime now = LocalDateTime.now();

        int started = activityMapper.updateStatusByTime(
                List.of(ActivityStatus.RECRUITING.code(), ActivityStatus.FULL.code()),
                ActivityStatus.ONGOING.code(), now);
        if (started > 0) {
            log.info("推进 {} 个活动状态为进行中", started);
        }

        int ended = activityMapper.updateEndedStatus(
                ActivityStatus.ONGOING.code(), ActivityStatus.ENDED.code(), now);
        if (ended > 0) {
            log.info("推进 {} 个活动状态为已结束", ended);
        }
    }
}
