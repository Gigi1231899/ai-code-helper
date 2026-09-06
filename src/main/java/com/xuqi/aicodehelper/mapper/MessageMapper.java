package com.xuqi.aicodehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuqi.aicodehelper.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageEntity> {
}
