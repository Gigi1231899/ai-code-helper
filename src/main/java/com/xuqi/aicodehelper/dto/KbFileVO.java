package com.xuqi.aicodehelper.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文件视图对象
 */
@Data
public class KbFileVO {

    /** 文件ID，删除时使用 */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传时间 */
    private LocalDateTime createTime;
}
