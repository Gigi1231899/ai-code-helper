package com.xuqi.aicodehelper.controller;

import com.xuqi.aicodehelper.common.Result;
import com.xuqi.aicodehelper.common.UserContext;
import com.xuqi.aicodehelper.dto.KbFileVO;
import com.xuqi.aicodehelper.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 私人知识库接口（GET/POST/DELETE 个人知识库文件）
 * <p>
 * 文件本体存在服务器 data/kb/{userId}/ 下，向量切片存在该用户专属向量库中，
 * 与其他用户完全隔离。
 */
@RestController
@RequestMapping("/knowledge-base")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文件到我的知识库
     *
     * @param file 表单字段名 file
     * @return 文件信息
     */
    @PostMapping("/files")
    public Result<KbFileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(knowledgeBaseService.upload(UserContext.getUserId(), file));
    }

    /**
     * 我的知识库文件列表
     *
     * @return 文件列表（按上传时间倒序）
     */
    @GetMapping("/files")
    public Result<List<KbFileVO>> list() {
        return Result.ok(knowledgeBaseService.listMine(UserContext.getUserId()));
    }

    /**
     * 删除我的知识库文件
     *
     * @param fileId 文件ID
     * @return 空
     */
    @DeleteMapping("/files/{fileId}")
    public Result<Void> delete(@PathVariable Long fileId) {
        knowledgeBaseService.delete(UserContext.getUserId(), fileId);
        return Result.ok();
    }
}
