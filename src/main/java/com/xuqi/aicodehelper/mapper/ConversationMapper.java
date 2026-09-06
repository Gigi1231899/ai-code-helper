package com.xuqi.aicodehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuqi.aicodehelper.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
