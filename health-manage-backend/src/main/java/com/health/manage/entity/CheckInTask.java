package com.health.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("check_in_task")
public class CheckInTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String taskName;
    private String icon;
    private String color;
    private String targetDesc;
    private LocalDateTime createTime;
}
