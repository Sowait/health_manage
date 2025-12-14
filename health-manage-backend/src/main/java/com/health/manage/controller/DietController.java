package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.DietRecord;
import com.health.manage.mapper.DietRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diet")
public class DietController {

    @Autowired
    private DietRecordMapper dietRecordMapper;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Long userId, @RequestParam String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate recordDate = LocalDate.parse(date);

        QueryWrapper<DietRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("record_date", recordDate);
        List<DietRecord> list = dietRecordMapper.selectList(query);
        
        Map<String, List<DietRecord>> meals = new HashMap<>();
        meals.put("breakfast", new ArrayList<>());
        meals.put("lunch", new ArrayList<>());
        meals.put("dinner", new ArrayList<>());
        meals.put("snack", new ArrayList<>());
        
        for (DietRecord record : list) {
            if (meals.containsKey(record.getMealType())) {
                meals.get(record.getMealType()).add(record);
            }
        }
        
        result.put("code", 200);
        result.put("data", meals);
        return result;
    }

    @GetMapping("/history")
    public Map<String, Object> history(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<DietRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (startDate != null && !startDate.isEmpty()) {
            query.ge("record_date", LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.le("record_date", LocalDate.parse(endDate));
        }
        query.orderByDesc("record_date");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DietRecord> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DietRecord> pageResult =
                dietRecordMapper.selectPage(p, query);

        result.put("code", 200);
        result.put("data", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody DietRecord record) {
        record.setCreateTime(LocalDateTime.now());
        dietRecordMapper.insert(record);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "添加成功");
        result.put("data", record);
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        dietRecordMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "删除成功");
        return result;
    }
}
