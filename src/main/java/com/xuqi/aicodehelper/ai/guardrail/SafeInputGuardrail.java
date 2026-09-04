package com.xuqi.aicodehelper.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Set;

/**
 * 安全输入防护栏类，用于验证用户输入是否包含敏感词汇
 * 实现了InputGuardrail接口，提供输入内容的安全检查功能
 */
public class SafeInputGuardrail implements InputGuardrail {

    // 定义敏感词汇集合，使用不可变Set保证线程安全
    private static final Set<String> sensetiveWords = Set.of("杀","kill");
    /**
     * 验证用户输入内容的方法
     * @param userMessage 包含用户输入信息的对象
     * @return InputGuardrailResult 验证结果，包含成功或失败信息
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage){
        // 将输入文本转换为小写，以便进行不区分大小写的敏感词检查
        String inputText=userMessage.singleText().toLowerCase();
        // 使用非单词字符作为分隔符，将输入文本分割成单词数组
        String[] words=inputText.split("\\W+");
        // 遍历所有单词，检查是否包含敏感词汇
        for(String word:words){
            // 如果发现敏感词汇，立即返回致命错误结果
            if(sensetiveWords.contains(word))
                return fatal("包含敏感词汇"+word);
        }
        // 如果未发现敏感词汇，返回成功结果
        return success();
    }

}
