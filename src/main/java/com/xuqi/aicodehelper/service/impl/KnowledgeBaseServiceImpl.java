package com.xuqi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuqi.aicodehelper.ai.AiServiceManager;
import com.xuqi.aicodehelper.ai.rag.UserEmbeddingStoreManager;
import com.xuqi.aicodehelper.common.BusinessException;
import com.xuqi.aicodehelper.config.AppProperties;
import com.xuqi.aicodehelper.dto.KbFileVO;
import com.xuqi.aicodehelper.entity.KbFile;
import com.xuqi.aicodehelper.mapper.KbFileMapper;
import com.xuqi.aicodehelper.service.KnowledgeBaseService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 私人知识库服务实现
 * <p>
 * 一次上传的完整链路：
 * 校验（后缀/大小/数量） -> 落盘到用户专属目录 -> 解析成 Document
 * -> 写入该用户的向量库（自动切片 + 向量化）-> 记录入库
 * -> 刷新该用户的 AI 服务缓存，让新文档立刻可被检索
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Resource
    private KbFileMapper kbFileMapper;

    @Resource
    private AppProperties appProperties;

    /** 每用户独立的向量库 */
    @Resource
    private UserEmbeddingStoreManager userEmbeddingStoreManager;

    /** 上传后需要让用户下次对话用上新检索器 */
    @Resource
    private AiServiceManager aiServiceManager;

    @Override
    public KbFileVO upload(Long userId, MultipartFile file) {
        // ① 空文件校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        // ② 文件名与后缀校验（cleanPath 防目录穿越）
        String originalName = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename(), "文件名不能为空"));
        String extension = getExtension(originalName);
        if (!getAllowedExtensions().contains(extension)) {
            throw new BusinessException("不支持的文件类型 ." + extension
                    + "，仅支持：" + appProperties.getKnowledgeBase().getAllowedExtensions());
        }

        // ③ 文件大小校验（技术文档一般几 MB，这里默认上限 10MB）
        long maxBytes = appProperties.getKnowledgeBase().getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件大小不能超过 "
                    + appProperties.getKnowledgeBase().getMaxFileSizeMb() + "MB");
        }

        // ④ 文件数量校验（每人最多 50 个）
        Long currentCount = kbFileMapper.selectCount(new LambdaQueryWrapper<KbFile>()
                .eq(KbFile::getUserId, userId));
        int maxFiles = appProperties.getKnowledgeBase().getMaxFilesPerUser();
        if (currentCount != null && currentCount >= maxFiles) {
            throw new BusinessException("知识库最多只能存放 " + maxFiles + " 个文件，请先删除部分文件");
        }

        // ⑤ 落盘到用户专属目录：{base-path}/kb/{userId}/{uuid}.{ext}
        Path target = null;
        try {
            Path dir = Paths.get(appProperties.getStorage().getBasePath(), "kb", String.valueOf(userId));
            Files.createDirectories(dir);
            String storeName = UUID.randomUUID() + "." + extension;
            target = dir.resolve(storeName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // ⑥ 解析文档并写入该用户的向量库（切片 + 向量化 + 落盘）
            Document document = parseDocument(target, extension);
            // 显式写入原始文件名元数据：① 检索时拼在正文前提高命中率 ② 删除文件时能精确定位切片
            document.metadata().put(Document.FILE_NAME, originalName);
            userEmbeddingStoreManager.ingest(userId, List.of(document));
            log.info("知识库上传成功：userId={}, fileName={}", userId, originalName);

            // ⑦ 数据库记录元信息
            KbFile entity = new KbFile();
            entity.setUserId(userId);
            entity.setFileName(originalName);
            entity.setStoreName(target.getFileName().toString());
            entity.setFileSize(file.getSize());
            entity.setCreateTime(LocalDateTime.now());
            kbFileMapper.insert(entity);

            // ⑧ 关键：让该用户下次对话使用新的检索器，否则读不到刚上传的文档
            aiServiceManager.invalidate(userId);

            return toVO(entity);
        } catch (BusinessException e) {
            // 业务校验失败：清理已经落盘的文件
            deleteQuietly(target);
            throw e;
        } catch (Exception e) {
            // 解析/向量化失败：同样清理，避免产生无记录的垃圾文件
            deleteQuietly(target);
            log.error("知识库上传失败：userId={}, fileName={}", userId, originalName, e);
            throw new BusinessException("文档解析失败，请确认文件未损坏或内容可读");
        }
    }

    @Override
    public List<KbFileVO> listMine(Long userId) {
        List<KbFile> files = kbFileMapper.selectList(new LambdaQueryWrapper<KbFile>()
                .eq(KbFile::getUserId, userId)
                .orderByDesc(KbFile::getCreateTime));
        return files.stream().map(this::toVO).toList();
    }

    @Override
    public void delete(Long userId, Long fileId) {
        // ① 校验归属
        KbFile kbFile = kbFileMapper.selectById(fileId);
        if (kbFile == null || !kbFile.getUserId().equals(userId)) {
            throw new BusinessException(404, "文件不存在或无权删除");
        }

        // ② 删除该文件对应的向量切片（按 file_name 元数据精确过滤，不影响其他文件）
        userEmbeddingStoreManager.removeByFileName(userId, kbFile.getFileName());

        // ③ 删除磁盘文件
        Path path = Paths.get(appProperties.getStorage().getBasePath(), "kb",
                String.valueOf(userId), kbFile.getStoreName());
        deleteQuietly(path);

        // ④ 删除数据库记录
        kbFileMapper.deleteById(fileId);

        // ⑤ 刷新检索器
        aiServiceManager.invalidate(userId);

        log.info("知识库文件删除成功：userId={}, fileId={}, fileName={}", userId, fileId, kbFile.getFileName());
    }

    /**
     * 按后缀选择对应的文档解析器
     *
     * @param path 文件绝对路径
     * @param ext  小写后缀
     * @return 解析后的 Document
     */
    private Document parseDocument(Path path, String ext) {
        DocumentParser parser = switch (ext) {
            // Markdown / 纯文本
            case "txt", "md" -> new TextDocumentParser();
            // Word 文档（.doc / .docx）
            case "doc", "docx" -> new ApachePoiDocumentParser();
            // PDF 文档
            case "pdf" -> new ApachePdfBoxDocumentParser();
            default -> throw new BusinessException("不支持的文件类型 ." + ext);
        };
        return FileSystemDocumentLoader.loadDocument(path, parser);
    }

    /**
     * 取文件后缀（小写，不含点）
     *
     * @param fileName 文件名
     * @return 后缀；无后缀时返回空串
     */
    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }

    /**
     * 允许上传的后缀集合
     *
     * @return 小写后缀集合
     */
    private List<String> getAllowedExtensions() {
        return List.of(appProperties.getKnowledgeBase().getAllowedExtensions().split(","));
    }

    /**
     * 静默删除文件（失败只记日志，不影响主流程）
     *
     * @param path 文件路径，允许为 null
     */
    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败：{}", path, e);
        }
    }

    /** 实体转 VO */
    private KbFileVO toVO(KbFile entity) {
        KbFileVO vo = new KbFileVO();
        vo.setId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
