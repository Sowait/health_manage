package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.CheckInRecord;
import com.health.manage.entity.CheckInTask;
import com.health.manage.mapper.CheckInRecordMapper;
import com.health.manage.mapper.CheckInTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/checkin")
public class CheckInController {

    @Autowired
    private CheckInTaskMapper taskMapper;
    @Autowired
    private CheckInRecordMapper recordMapper;

    @GetMapping("/tasks")
    public Map<String, Object> getTasks(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        // Get all tasks for user
        QueryWrapper<CheckInTask> taskQuery = new QueryWrapper<>();
        taskQuery.eq("user_id", userId);
        List<CheckInTask> tasks = taskMapper.selectList(taskQuery);
        
        // Get today's records
        QueryWrapper<CheckInRecord> recordQuery = new QueryWrapper<>();
        recordQuery.eq("user_id", userId).eq("record_date", LocalDate.now());
        List<CheckInRecord> todayRecords = recordMapper.selectList(recordQuery);
        List<Long> completedTaskIds = todayRecords.stream()
                .map(CheckInRecord::getTaskId)
                .collect(Collectors.toList());

        // Combine
        List<Map<String, Object>> taskList = tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", task.getId());
            map.put("name", task.getTaskName());
            map.put("desc", task.getTargetDesc());
            map.put("icon", task.getIcon());
            map.put("color", task.getColor());
            map.put("completed", completedTaskIds.contains(task.getId()));
            return map;
        }).collect(Collectors.toList());

        result.put("code", 200);
        result.put("data", taskList);
        return result;
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        Long taskId = Long.valueOf(payload.get("taskId").toString());
        
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<CheckInRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId)
             .eq("task_id", taskId)
             .eq("record_date", LocalDate.now());
        
        CheckInRecord record = recordMapper.selectOne(query);
        if (record != null) {
            recordMapper.deleteById(record.getId());
            result.put("status", false);
        } else {
            CheckInRecord newRecord = new CheckInRecord();
            newRecord.setUserId(userId);
            newRecord.setTaskId(taskId);
            newRecord.setRecordDate(LocalDate.now());
            newRecord.setCreateTime(LocalDateTime.now());
            recordMapper.insert(newRecord);
            result.put("status", true);
        }
        
        result.put("code", 200);
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
        QueryWrapper<CheckInRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (startDate != null && !startDate.isEmpty()) {
            query.ge("record_date", LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            query.le("record_date", LocalDate.parse(endDate));
        }
        query.orderByDesc("record_date");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CheckInRecord> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<CheckInRecord> pageResult =
                recordMapper.selectPage(p, query);

        List<CheckInRecord> records = pageResult.getRecords();
        List<Long> ids = records.stream().map(CheckInRecord::getTaskId).distinct().collect(Collectors.toList());
        Map<Long, String> nameMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<CheckInTask> tasks = taskMapper.selectList(new QueryWrapper<CheckInTask>().in("id", ids));
            for (CheckInTask t : tasks) nameMap.put(t.getId(), t.getTaskName());
        }
        List<Map<String, Object>> out = records.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("taskId", r.getTaskId());
            m.put("recordDate", r.getRecordDate());
            m.put("createTime", r.getCreateTime());
            m.put("taskName", nameMap.getOrDefault(r.getTaskId(), null));
            return m;
        }).collect(Collectors.toList());

        result.put("code", 200);
        result.put("data", out);
        result.put("total", pageResult.getTotal());
        return result;
    }

    @PostMapping("/task/add")
    public Map<String, Object> addTask(@RequestBody CheckInTask task) {
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "添加成功");
        return result;
    }
}
