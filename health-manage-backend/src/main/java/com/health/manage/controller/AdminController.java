package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.*;
import com.health.manage.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserMapper userMapper;
    @Autowired private HealthDataMapper healthDataMapper;
    @Autowired private CheckInRecordMapper checkInRecordMapper;
    @Autowired private DietRecordMapper dietRecordMapper;
    @Autowired private CheckInTaskTemplateMapper taskTemplateMapper;
    @Autowired private CheckInTaskMapper checkInTaskMapper;
    @Autowired private ReminderTemplateMapper reminderTemplateMapper;

    private boolean isAdmin(HttpSession session) {
        Object obj = session.getAttribute("user");
        if (obj instanceof User) {
            User u = (User) obj;
            return "ADMIN".equalsIgnoreCase(u.getRole());
        }
        return false;
    }

    @GetMapping("/users")
    public Map<String, Object> listUsers(HttpSession session,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        QueryWrapper<User> q = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            q.like("username", keyword).or().like("nickname", keyword);
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pr = userMapper.selectPage(p, q);
        result.put("code", 200);
        result.put("data", pr.getRecords());
        result.put("total", pr.getTotal());
        return result;
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> editUser(HttpSession session, @PathVariable Long id, @RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        user.setId(id);
        userMapper.updateById(user);
        result.put("code", 200);
        return result;
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(HttpSession session, @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        userMapper.deleteById(id);
        result.put("code", 200);
        return result;
    }

    @GetMapping("/dashboard/overview")
    public Map<String, Object> adminOverview(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        Long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        LocalDate today = LocalDate.now();
        Long todayHealth = healthDataMapper.selectCount(new QueryWrapper<HealthData>().eq("record_date", today));
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("todayHealthRecords", todayHealth);
        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @GetMapping("/dashboard/trend")
    public Map<String, Object> adminTrend(HttpSession session,
                                          @RequestParam(defaultValue = "weight") String metric,
                                          @RequestParam(defaultValue = "30") Integer days) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1);
        List<HealthData> list = healthDataMapper.selectList(new QueryWrapper<HealthData>()
                .ge("record_date", start).le("record_date", today).orderByAsc("record_date"));
        Map<String, List<HealthData>> byDay = new HashMap<>();
        for (HealthData h : list) {
            String key = h.getRecordDate().toString();
            byDay.computeIfAbsent(key, k -> new ArrayList<>()).add(h);
        }
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            List<HealthData> day = byDay.getOrDefault(d.toString(), Collections.emptyList());
            Double val = null;
            if (!day.isEmpty()) {
                if ("weight".equals(metric)) {
                    val = day.stream().filter(h -> h.getWeight() != null).mapToDouble(h -> h.getWeight().doubleValue()).average().orElse(Double.NaN);
                } else if ("bp".equals(metric)) {
                    val = day.stream().filter(h -> h.getSystolic() != null).mapToInt(HealthData::getSystolic).average().orElse(Double.NaN);
                } else if ("steps".equals(metric)) {
                    val = day.stream().filter(h -> h.getSteps() != null).mapToInt(HealthData::getSteps).average().orElse(Double.NaN);
                }
            }
            Map<String, Object> item = new HashMap<>();
            item.put("recordDate", d.toString());
            item.put("value", (val != null && !val.isNaN()) ? val : null);
            series.add(item);
        }
        result.put("code", 200);
        result.put("data", series);
        return result;
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        LocalDate start = LocalDate.now().minusDays(7);
        List<HealthData> list = healthDataMapper.selectList(new QueryWrapper<HealthData>().ge("record_date", start));
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (HealthData h : list) {
            boolean abnormal = false;
            if (h.getHeartRate() != null && h.getHeartRate() > 100) abnormal = true;
            if (h.getSystolic() != null && h.getSystolic() > 140) abnormal = true;
            if (abnormal) {
                Map<String, Object> a = new HashMap<>();
                a.put("userId", h.getUserId());
                a.put("recordDate", h.getRecordDate().toString());
                a.put("heartRate", h.getHeartRate());
                a.put("systolic", h.getSystolic());
                alerts.add(a);
            }
        }
        result.put("code", 200);
        result.put("data", alerts);
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        long totalCheckins = checkInRecordMapper.selectCount(new QueryWrapper<CheckInRecord>());
        long totalDiets = dietRecordMapper.selectCount(new QueryWrapper<DietRecord>());
        long totalHealth = healthDataMapper.selectCount(new QueryWrapper<HealthData>());

        long todayRegistered = userMapper.selectCount(new QueryWrapper<User>().ge("create_time", start).lt("create_time", end));
        long todayCheckins = checkInRecordMapper.selectCount(new QueryWrapper<CheckInRecord>().eq("record_date", today));
        long todayDiets = dietRecordMapper.selectCount(new QueryWrapper<DietRecord>().eq("record_date", today));
        long todayHealth = healthDataMapper.selectCount(new QueryWrapper<HealthData>().eq("record_date", today));

        QueryWrapper<HealthData> abnormalQ = new QueryWrapper<>();
        abnormalQ.and(q -> q.gt("heart_rate", 100)
                .or().lt("heart_rate", 50)
                .or().gt("systolic", 140)
                .or().lt("systolic", 90)
                .or().gt("diastolic", 90)
                .or().lt("diastolic", 60)
                .or().gt("blood_sugar", 7.8)
                .or().lt("blood_sugar", 3.9));
        long abnormalCount = healthDataMapper.selectCount(abnormalQ);
        long alarmedCount = healthDataMapper.selectCount(new QueryWrapper<HealthData>().eq("alarm_status", 1));

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> totals = new HashMap<>();
        totals.put("users", totalUsers);
        totals.put("checkins", totalCheckins);
        totals.put("diets", totalDiets);
        totals.put("healthRecords", totalHealth);
        Map<String, Object> todayMap = new HashMap<>();
        todayMap.put("registered", todayRegistered);
        todayMap.put("checkins", todayCheckins);
        todayMap.put("diets", todayDiets);
        todayMap.put("healthRecords", todayHealth);
        Map<String, Object> healthAlert = new HashMap<>();
        healthAlert.put("total", totalHealth);
        healthAlert.put("abnormal", abnormalCount);
        healthAlert.put("alarmed", alarmedCount);
        data.put("totals", totals);
        data.put("today", todayMap);
        data.put("healthAlert", healthAlert);

        result.put("code", 200);
        result.put("data", data);
        return result;
    }

    @GetMapping("/task-templates")
    public Map<String, Object> listTemplates(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        result.put("code", 200);
        result.put("data", taskTemplateMapper.selectList(new QueryWrapper<>()));
        return result;
    }

    @PostMapping("/task-templates")
    public Map<String, Object> addTemplate(HttpSession session, @RequestBody CheckInTaskTemplate t) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        taskTemplateMapper.insert(t);
        result.put("code", 200);
        return result;
    }

    @PutMapping("/task-templates/{id}")
    public Map<String, Object> editTemplate(HttpSession session, @PathVariable Long id, @RequestBody CheckInTaskTemplate t) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        t.setId(id);
        taskTemplateMapper.updateById(t);
        result.put("code", 200);
        return result;
    }

    @DeleteMapping("/task-templates/{id}")
    public Map<String, Object> deleteTemplate(HttpSession session, @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        taskTemplateMapper.deleteById(id);
        result.put("code", 200);
        return result;
    }

    @PostMapping("/task-templates/apply")
    public Map<String, Object> applyTemplates(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        List<User> users = userMapper.selectList(new QueryWrapper<>());
        List<CheckInTaskTemplate> templates = taskTemplateMapper.selectList(new QueryWrapper<>());
        for (User u : users) {
            for (CheckInTaskTemplate t : templates) {
                QueryWrapper<CheckInTask> existsQ = new QueryWrapper<>();
                existsQ.eq("user_id", u.getId()).eq("task_name", t.getTaskName());
                if (checkInTaskMapper.selectCount(existsQ) > 0) continue;
                CheckInTask task = new CheckInTask();
                task.setUserId(u.getId());
                task.setTaskName(t.getTaskName());
                task.setIcon(t.getIcon());
                task.setColor(t.getColor());
                task.setTargetDesc(t.getTargetDesc());
                task.setCreateTime(java.time.LocalDateTime.now());
                checkInTaskMapper.insert(task);
            }
        }
        result.put("code", 200);
        return result;
    }

    @GetMapping("/health-records")
    public Map<String, Object> listHealthRecords(HttpSession session,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        QueryWrapper<HealthData> q = new QueryWrapper<>();
        if (userId != null) q.eq("user_id", userId);
        if (startDate != null && !startDate.isEmpty()) q.ge("record_date", java.time.LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty()) q.le("record_date", java.time.LocalDate.parse(endDate));
        q.orderByDesc("record_date");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<HealthData> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<HealthData> pr = healthDataMapper.selectPage(p, q);

        List<HealthData> list = pr.getRecords();
        Set<Long> uids = new HashSet<>();
        for (HealthData h : list) { if (h.getUserId() != null) uids.add(h.getUserId()); }
        Map<Long, String> nickMap = new HashMap<>();
        if (!uids.isEmpty()) {
            List<User> users = userMapper.selectList(new QueryWrapper<User>().in("id", uids));
            for (User u : users) nickMap.put(u.getId(), (u.getNickname() != null && !u.getNickname().isEmpty()) ? u.getNickname() : u.getUsername());
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (HealthData h : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("userId", h.getUserId());
            m.put("nickname", nickMap.getOrDefault(h.getUserId(), null));
            m.put("recordDate", h.getRecordDate());
            m.put("weight", h.getWeight());
            m.put("heartRate", h.getHeartRate());
            m.put("systolic", h.getSystolic());
            m.put("diastolic", h.getDiastolic());
            m.put("steps", h.getSteps());
            m.put("bloodSugar", h.getBloodSugar());
            m.put("alarmStatus", h.getAlarmStatus());
            out.add(m);
        }

        result.put("code", 200);
        result.put("data", out);
        result.put("total", pr.getTotal());
        return result;
    }

    @PostMapping("/health-records/{id}/alarm")
    public Map<String, Object> markAlarm(HttpSession session, @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        HealthData d = healthDataMapper.selectById(id);
        if (d == null) { result.put("code", 404); return result; }
        d.setAlarmStatus(1);
        healthDataMapper.updateById(d);
        result.put("code", 200);
        return result;
    }

    @GetMapping("/reminders")
    public Map<String, Object> listReminders(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        result.put("code", 200);
        result.put("data", reminderTemplateMapper.selectList(new QueryWrapper<>()));
        return result;
    }

    @PostMapping("/reminders")
    public Map<String, Object> addReminder(HttpSession session, @RequestBody ReminderTemplate t) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        reminderTemplateMapper.insert(t);
        result.put("code", 200);
        return result;
    }

    @PutMapping("/reminders/{id}")
    public Map<String, Object> editReminder(HttpSession session, @PathVariable Long id, @RequestBody ReminderTemplate t) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        t.setId(id);
        reminderTemplateMapper.updateById(t);
        result.put("code", 200);
        return result;
    }

    @DeleteMapping("/reminders/{id}")
    public Map<String, Object> deleteReminder(HttpSession session, @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if (!isAdmin(session)) { result.put("code", 403); return result; }
        reminderTemplateMapper.deleteById(id);
        result.put("code", 200);
        return result;
    }
}
