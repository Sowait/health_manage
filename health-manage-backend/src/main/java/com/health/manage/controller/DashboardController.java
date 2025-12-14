package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.HealthData;
import com.health.manage.mapper.HealthDataMapper;
import com.health.manage.entity.DietRecord;
import com.health.manage.mapper.DietRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private HealthDataMapper healthDataMapper;
    @Autowired
    private DietRecordMapper dietRecordMapper;

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        QueryWrapper<HealthData> qLatest = new QueryWrapper<>();
        qLatest.eq("user_id", userId).orderByDesc("record_date").last("limit 1");
        HealthData latest = healthDataMapper.selectOne(qLatest);

        QueryWrapper<HealthData> q7 = new QueryWrapper<>();
        q7.eq("user_id", userId).ge("record_date", today.minusDays(6)).le("record_date", today);
        List<HealthData> last7 = healthDataMapper.selectList(q7);

        QueryWrapper<DietRecord> qDiet7 = new QueryWrapper<>();
        qDiet7.eq("user_id", userId).ge("record_date", today.minusDays(6)).le("record_date", today);
        List<DietRecord> diet7 = dietRecordMapper.selectList(qDiet7);

        int steps7 = last7.stream().mapToInt(h -> h.getSteps() == null ? 0 : h.getSteps()).sum();
        int calories7 = diet7.stream().mapToInt(d -> d.getCalories() == null ? 0 : d.getCalories()).sum();

        Map<String, Object> data = new HashMap<>();
        data.put("latest", latest);
        data.put("steps7", steps7);
        data.put("calories7", calories7);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam Long userId, @RequestParam(defaultValue = "7") Integer days) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1);

        QueryWrapper<HealthData> q = new QueryWrapper<>();
        q.eq("user_id", userId).ge("record_date", start).le("record_date", today).orderByAsc("record_date");
        List<HealthData> list = healthDataMapper.selectList(q);

        Map<String, HealthData> byDate = new HashMap<>();
        for (HealthData h : list) {
            byDate.put(h.getRecordDate().toString(), h);
        }

        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            HealthData h = byDate.get(d.toString());
            Map<String, Object> item = new HashMap<>();
            item.put("recordDate", d.toString());
            item.put("weight", h == null ? null : h.getWeight());
            item.put("systolic", h == null ? null : h.getSystolic());
            series.add(item);
        }

        result.put("code", 200);
        result.put("data", series);
        return result;
    }
}

