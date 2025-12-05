package com.health.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.health.manage.entity.User;
import com.health.manage.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        query.eq("password", user.getPassword());
        User dbUser = userMapper.selectOne(query);

        if (dbUser != null) {
            session.setAttribute("user", dbUser);
            result.put("code", 200);
            result.put("msg", "登录成功");
            result.put("data", dbUser);
        } else {
            result.put("code", 400);
            result.put("msg", "用户名或密码错误");
        }
        return result;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        if (userMapper.selectCount(query) > 0) {
            result.put("code", 400);
            result.put("msg", "用户名已存在");
            return result;
        }

        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        result.put("code", 200);
        result.put("msg", "注册成功");
        return result;
    }
    
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "退出成功");
        return result;
    }
}
