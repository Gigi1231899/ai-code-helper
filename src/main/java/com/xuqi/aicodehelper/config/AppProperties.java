package com.xuqi.aicodehelper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用自定义配置（对应 application*.yml 中的 app 节点）
 * <p>
 * 集中管理文件落盘路径与知识库上传规则，方便部署时用环境变量覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 文件存储相关 */
    private Storage storage = new Storage();

    /** 知识库上传限制 */
    private KnowledgeBase knowledgeBase = new KnowledgeBase();

    /** 文件落盘配置 */
    @Data
    public static class Storage {
        /** 存储根目录，相对路径以进程工作目录为基准 */
        private String basePath = "data";
    }

    /** 私人知识库规则 */
    @Data
    public static class KnowledgeBase {
        /** 单文件大小上限（MB） */
        private int maxFileSizeMb = 10;
        /** 每个用户最多文件数 */
        private int maxFilesPerUser = 50;
        /** 允许上传的后缀，逗号分隔 */
        private String allowedExtensions = "txt,md,docx,doc,pdf";
    }
}
