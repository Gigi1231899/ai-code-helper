package com.xuqi.aicodehelper.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户私人知识库文件（对应表 t_kb_file）
 * <p>
 * 文件本体落盘到 data/kb/{userId}/{storeName}，数据库只存元信息。
 */
@Data
@TableName("t_kb_file")
public class KbFile {

    /** 文件ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID（每个用户一个独立知识库） */
    private Long userId;

    /** 原始文件名，用于列表展示；同时作为向量元数据用于删除时过滤 */
    private String fileName;

    /** 落盘文件名（UUID），避免同名文件互相覆盖 */
    private String storeName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传时间 */
    private LocalDateTime createTime;
}
