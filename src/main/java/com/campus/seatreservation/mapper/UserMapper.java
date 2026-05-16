package com.campus.seatreservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.seatreservation.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层 — 操作 user 表
 *
 * 继承 BaseMapper<User> 后，MyBatis-Plus 自动提供 CRUD 方法。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
