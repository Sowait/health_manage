package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.HealthData;
import com.health.manage.mapper.HealthDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthDataController {

    @Autowired
    private HealthDataMapper healthDataMapper;

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<HealthData> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (startDate != null && !startDate.isEmpty()) {
            query.ge("record_date", LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.le("record_date", LocalDate.parse(endDate));
        }
        query.orderByDesc("record_date");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<HealthData> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<HealthData> pageResult =
                healthDataMapper.selectPage(p, query);

        result.put("code", 200);
        result.put("data", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody HealthData data) {
        Map<String, Object> result = new HashMap<>();
        
        // 检查该用户该日期是否已有记录
        QueryWrapper<HealthData> query = new QueryWrapper<>();
        query.eq("user_id", data.getUserId());
        query.eq("record_date", data.getRecordDate());
        HealthData exist = healthDataMapper.selectOne(query);

        if (exist != null) {
            // 如果已存在，更新该记录
            data.setId(exist.getId());
            healthDataMapper.updateById(data);
        } else {
            // 不存在，执行插入
            data.setCreateTime(LocalDateTime.now());
            healthDataMapper.insert(data);
        }
        
        result.put("code", 200);
        result.put("msg", "保存成功");
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        healthDataMapper.deleteById(id);
        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }
}
