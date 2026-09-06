package com.xuqi.aicodehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuqi.aicodehelper.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * <p>
 * 继承 BaseMapper 后即拥有单表 CRUD；用户名唯一性由数据库唯一索引兜底。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
