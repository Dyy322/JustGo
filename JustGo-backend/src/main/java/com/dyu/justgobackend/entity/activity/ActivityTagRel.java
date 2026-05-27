package com.dyu.justgobackend.entity.activity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("activity_tag_rel")
public class ActivityTagRel {

    @TableField("activity_id")
    private Long activityId;

    @TableField("tag_id")
    private Long tagId;
}
