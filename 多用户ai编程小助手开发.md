后端



## 对话标题，消息内容，大模型记忆体，用户，知识库文件存储信息（5个）Mysql持久化

配置datasource，mybatis-plus的属性，自动装配mybatisautoconfiguration,注册sqlsessionfactory,sqlsessiontemplate,mapperscanfactory（生成代理对象)



Spring Boot 启动
    ↓
@SpringBootApplication 扫描
    ↓
MybatisPlusAutoConfiguration 加载
    ↓

│ 1. 读取 MybatisPlusProperties（从 application.yml）        │
│    → mybatis-plus.configuration.log-impl = StdOutImpl     │

│ 2. 创建 SqlSessionFactory                                   
│    → MybatisSqlSessionFactoryBean                          
│    → 设置 DataSource（HikariCP）                           
│    → 设置 Configuration（包含 log-impl）                   
│    → 扫描 Mapper XML 文件                                  

│ 3. 创建 SqlSessionTemplate                                  │
│    → 线程安全的 SqlSession 包装器                           │

│ 4. 扫描 @Mapper 接口                                       │
│    → MapperScannerConfigurer                               │
│    → 为每个 Mapper 生成动态代理（MapperProxy）             │

    ↓
Bean 注册完成
    ↓
用户请求 → Controller → Service → Mapper 方法调用
    ↓
MapperProxy 拦截 → SqlSessionTemplate → DefaultSqlSession
    ↓
执行 SQL → Configuration 中的 Log 实现 → StdOutImpl.debug()
    ↓
输出日志到控制台

```yml
# 数据源驱动，连接地址/账号密码放在各环境配置里
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driverspring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_code_helper?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    # TODO: 改成你自己本机 MySQL 的 root 密码
    password: 1234


mybatis-plus:
  configuration:
    # 下划线转驼峰：create_time -> createTime
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # 主键使用数据库自增，保证会话ID是递增的 int
      id-type: auto
# 本地开发打印 SQL，方便排查
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 部署不需要打印SQL
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl


```





conversation持久化

数据库

![](C:\Users\25380\AppData\Roaming\marktext\images\2026-09-05-10-47-47-image.png)

![](C:\Users\25380\AppData\Roaming\marktext\images\2026-09-05-22-25-25-image.png)

联合索引（二级索引）（user_id,update_time) 加速查询，B+树平衡而且很矮，这里需要用叶子结点的(user_id,update_time,id)的主键id来回表找到数据行





message持久化

联合索引（conversation_id,id)加速查询



kb_file持久化

普通索引 user_id



chatmemory持久化

唯一索引 memory_id

```sql
CREATE TABLE IF NOT EXISTS t_conversation (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID（int，前端用它区分不同对话）',
    user_id     BIGINT      NOT NULL COMMENT '所属用户ID',
    title       VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    mode        VARCHAR(20) NOT NULL COMMENT '对话模式：rag=知识库 / mcp=联网搜索 / mix=混合（创建后不可修改）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    KEY idx_user_update (user_id, update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='会话表';
```

```java
@Override
    public List<ConversationVO> listMine(Long userId) {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .orderByDesc(Conversation::getUpdateTime)
        );
        return conversations.stream().map(this::toVO).toList();
    }
```

chat-memory模型对话记忆体持久化

```sql
-- ----------------------------
-- 5. 对话记忆表（LangChain4j ChatMemory 持久化，重启不丢上下文）
--    与 t_message 的区别：这里存的是「大模型视角」的完整消息（含工具调用消息），
--    t_message 只存给用户看的一问一答。
-- ----------------------------
CREATE TABLE IF NOT EXISTS t_chat_memory (
    id          BIGINT    PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    memory_id   BIGINT    NOT NULL COMMENT '记忆ID，直接复用会话ID（保证多用户隔离）',
    content     LONGTEXT  COMMENT 'JSON 序列化的 ChatMessage 列表',
    update_time DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_memory_id (memory_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='对话记忆持久化表';
```



Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@4fddc8ee] was not registered for synchronization because synchronization is not active
2026-09-05T10:26:17.640+08:00  INFO 29492 --- [ai-code-helper] [nio-8090-exec-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2026-09-05T10:26:17.929+08:00  INFO 29492 --- [ai-code-helper] [nio-8090-exec-1] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@47d72f37
2026-09-05T10:26:17.933+08:00  INFO 29492 --- [ai-code-helper] [nio-8090-exec-1] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
JDBC Connection [HikariProxyConnection@1988749709 wrapping com.mysql.cj.jdbc.ConnectionImpl@47d72f37] will not be managed by Spring
==>  Preparing: SELECT id,user_id,title,mode,create_time,update_time FROM t_conversation WHERE (user_id = ?) ORDER BY update_time DESC
==> Parameters: 2(Long)
<==    Columns: id, user_id, title, mode, create_time, update_time
<==        Row: 4, 2, 如果明天面试，面试官看着我的简历会说什么，问什么，我自己要怎..., rag, 2026-09-04 17:09:55, 2026-09-04 17:14:39
<==        Row: 3, 2, 你说我的OJ项目面试官会问什么内容, mix, 2026-09-04 17:00:01, 2026-09-04 17:07:34
<==      Total: 2
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@4fddc8ee]



## 多用户隔离

aiServiceManager内部缓存userId->ragservice代理对象，ragservice代理对象有service工厂创建



### chatMemory多用户隔离

```java
case "rag" -> aiServiceManager.getRagService(userId).chatRag(memoryId, message);

aiServiceManager管理RagService代理对象
=======================================================================
 public RagService getRagService(Long userId) {
//        如果指定的 key 不存在，就计算一个值并放入 Map；如果已存在，直接返回现有值。
        return ragServiceCache.computeIfAbsent(userId,
                id -> aiCodeHelperServiceFactory.createRagService(ragConfig.createRetriever(id)));
    }
aiCodeHelperServiceFactory创建RagService代理对象
=======================================================================
    public RagService createRagService(ContentRetriever contentRetriever) {
        return AiServices.builder(RagService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(streamingChatModel)
引用方法createChatMemory
                .chatMemoryProvider(this::createChatMemory)
                .contentRetriever(contentRetriever)
                .build();
    }



/**
     * 记忆提供者：按 memoryId（即会话ID）为单位创建独立的滑动窗口记忆
     * <p>
     * 底层存储走 MySQL，所以每个会话的上下文既互相隔离，又能在重启后恢复。
     *
     * @param memoryId 记忆ID（会话ID）
     * @return 会话记忆
     */
    private MessageWindowChatMemory createChatMemory(Object memoryId) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
大模型记忆体存储，实现ChatMemoryStore。
build动态代理对象时，根据memoryId拿chatmemory（MessageWindowChatMemory）
，chatmemory 的add msg实际保存到store的updatemessage。
=======================================================================
@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ChatMemoryMapper chatMemoryMapper;

    /**
     * 读取某个会话的历史消息
     *
     * @param memoryId 记忆ID（这里就是会话ID）
     * @return 历史消息列表，无记录时返回空列表（不能返回 null，否则 LangChain4j 会 NPE）
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Long id = toLong(memoryId);
        ChatMemoryEntity entity = selectByMemoryId(id);
        if (entity == null || !StringUtils.hasText(entity.getContent())) {
            return List.of();
        }
        // 用官方序列化器还原消息（支持 UserMessage / AiMessage / ToolExecutionResultMessage）
        return ChatMessageDeserializer.messagesFromJson(entity.getContent());
    }

    /**
     * 覆盖写入某个会话的历史消息
     *
     * @param memoryId 记忆ID（会话ID）
     * @param messages 完整的历史消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long id = toLong(memoryId);
        String json = ChatMessageSerializer.messagesToJson(messages);

        ChatMemoryEntity entity = selectByMemoryId(id);
        if (entity == null) {
            // 首次写入：新增记录
            ChatMemoryEntity newEntity = new ChatMemoryEntity();
            newEntity.setMemoryId(id);
            newEntity.setContent(json);
            newEntity.setUpdateTime(LocalDateTime.now());
            chatMemoryMapper.insert(newEntity);
        } else {
            // 后续写入：更新内容
            ChatMemoryEntity updateEntity = new ChatMemoryEntity();
            updateEntity.setId(entity.getId());
            updateEntity.setContent(json);
            updateEntity.setUpdateTime(LocalDateTime.now());
            chatMemoryMapper.updateById(updateEntity);
        }
    }

    /**
     * 删除某个会话的记忆（删除会话时调用）
     *
     * @param memoryId 记忆ID（会话ID）
     */
    @Override
    public void deleteMessages(Object memoryId) {
        Long id = toLong(memoryId);
        chatMemoryMapper.delete(new LambdaQueryWrapper<ChatMemoryEntity>()
                .eq(ChatMemoryEntity::getMemoryId, id));
    }

    /**
     * 按 memoryId 查询记录
     *
     * @param memoryId 记忆ID
     * @return 记录；不存在返回 null
     */
    private ChatMemoryEntity selectByMemoryId(Long memoryId) {
        return chatMemoryMapper.selectOne(new LambdaQueryWrapper<ChatMemoryEntity>()
                .eq(ChatMemoryEntity::getMemoryId, memoryId));
    }

    /**
     * 把 memoryId 统一转成 Long
     * <p>
     * LangChain4j 传入的是 Object（@MemoryId 声明的是什么类型就是什么类型），
     * 这里兼容 int / Integer / Long / String 几种情况。
     *
     * @param memoryId 记忆ID
     * @return Long 类型的ID
     */
    private Long toLong(Object memoryId) {
        if (memoryId instanceof Number number) {
            return number.longValue();
        }
        if (memoryId instanceof String str) {
            return Long.valueOf(str);
        }
        throw new IllegalArgumentException("不支持的 memoryId 类型：" + memoryId);
    }
}
=======================================================================
@InputGuardrails(SafeInputGuardrail.class)
public interface RagService {

    @SystemMessage(fromResource= "rag-prompt.txt")
    Flux<String> chatRag(@MemoryId int memoryId,@UserMessage String message);

}



createRagService() 时确实"没拿到" memoryId，因为它只是在"注册"创建规则
（方法引用）。真正的 memoryId 是在 chatRag() 被调用时，
由框架从 @MemoryId 参数中读取，然后自动传给 createChatMemory() 的。
你看不到传递过程，因为它是框架内部完成的。
```





### 多用户RAG内容检索器隔离

```java
======UserEmbeddingStoreManager管理用户向量库====

public EmbeddingStore<TextSegment> getStore(Long userId) {
        return storeCache.computeIfAbsent(userId, this::loadOrCreate);
    }

private EmbeddingStore<TextSegment> loadOrCreate(Long userId) {
        Path path = storePath(userId);
        if (Files.exists(path)) {
            try {
                EmbeddingStore<TextSegment> loaded =
                        (EmbeddingStore<TextSegment>) InMemoryEmbeddingStore.fromFile(path);
                log.info("加载用户向量库：userId={}, path={}", userId, path);
                return loaded;
            } catch (Exception e) {
                log.error("向量库加载失败，将创建空库：userId={}, path={}", userId, path, e);
            }
        }
        return new InMemoryEmbeddingStore<>();
    }
=======EmbeddingService=======================

// ⑥ 解析文档并写入该用户的向量库（切片 + 向量化 + 落盘）
            Document document = parseDocument(target, extension);
            // 显式写入原始文件名元数据：① 检索时拼在正文前提高命中率 ② 删除文件时能精确定位切片
            document.metadata().put(Document.FILE_NAME, originalName);
            userEmbeddingStoreManager.ingest(userId, List.of(document));
            log.info("知识库上传成功：userId={}, fileName={}", userId, originalName);
// ⑧ 关键：让该用户下次对话使用新的检索器，否则读不到刚上传的文档
            aiServiceManager.invalidate(userId);

==========入库存储把文件解析切片=============================
public synchronized int ingest(Long userId, List<Document> documents) {
        EmbeddingStore<TextSegment> store = getStore(userId);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                // 按段落切分：每段最多 1000 字符，相邻段重叠 200 字符，避免语义被截断
                .documentSplitter(new DocumentByParagraphSplitter(1000, 200))
                // 把文件名拼到切片正文前，提升检索命中率，也方便回答时标注来源
                .textSegmentTransformer(segment -> TextSegment.from(
                        segment.metadata().getString("file_name") + "\n" + segment.text(),
                        segment.metadata()))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(documents);
        persist(userId);
        log.info("知识库摄入完成：userId={}, 文档数={}", userId, documents.size());
        return documents.size();
    }




```
