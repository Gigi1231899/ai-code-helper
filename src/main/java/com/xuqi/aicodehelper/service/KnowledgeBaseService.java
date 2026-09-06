package com.xuqi.aicodehelper.service;

import com.xuqi.aicodehelper.dto.KbFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 私人知识库服务：上传、列表、删除
 * <p>
 * 规则（可在 application*.yml 的 app.knowledge-base 中调整）：
 * - 每个用户一个独立知识库目录
 * - 单文件不超过 10MB
 * - 每个用户最多 50 个文件
 */
public interface KnowledgeBaseService {

    /**
     * 上传文档并写入用户向量库
     *
     * @param userId 用户ID
     * @param file   上传的文件
     * @return 文件信息
     */
    KbFileVO upload(Long userId, MultipartFile file);

    /**
     * 查询我的知识库文件列表
     *
     * @param userId 用户ID
     * @return 文件列表
     */
    List<KbFileVO> listMine(Long userId);

    /**
     * 删除文件（同步删除磁盘文件、向量切片与数据库记录）
     *
     * @param userId 用户ID
     * @param fileId 文件ID
     */
    void delete(Long userId, Long fileId);
}
