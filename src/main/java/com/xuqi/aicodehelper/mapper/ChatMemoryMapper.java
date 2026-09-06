package com.xuqi.aicodehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuqi.aicodehelper.entity.ChatMemoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话记忆 Mapper
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemoryEntity> {
}
