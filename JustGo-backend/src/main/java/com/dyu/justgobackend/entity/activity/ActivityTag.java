package com.dyu.justgobackend.entity.activity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_tag")
public class ActivityTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
