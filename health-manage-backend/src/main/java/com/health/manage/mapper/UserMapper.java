package com.health.manage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.health.manage.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
