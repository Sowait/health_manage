package com.health.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reminder_template")
public class ReminderTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String content;
    private String triggerTime;
    private Integer enabled;
    private LocalDateTime createTime;
}
