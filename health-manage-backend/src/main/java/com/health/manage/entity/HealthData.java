package com.health.manage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_data")
public class HealthData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal weight;
    private Integer heartRate;
    private Integer systolic;
    private Integer diastolic;
    private Integer steps;
    private BigDecimal bloodSugar;
    private LocalDate recordDate;
    private LocalDateTime createTime;
    private Integer alarmStatus;
}
