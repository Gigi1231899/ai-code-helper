package com.xuqi.aicodehelper.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;
//    @Test
//    public void testMultiUserChat() {
//        // 用户 A 的对话
//        String userAId = "user-001";
//        System.out.println("=== 用户 A 开始对话 ===");
//        System.out.println("用户A: 我叫张三");
//        System.out.println("AI: " + aiCodeHelperService.chat(userAId, "我叫张三"));
//
//        System.out.println("\n用户A: 我叫什么名字？");
//        System.out.println("AI: " + aiCodeHelperService.chat(userAId, "我叫什么名字？"));
//        // 预期：AI 能回答出 "张三"，因为有记忆
//
//        // 用户 B 的对话（完全独立的记忆空间）
//        String userBId = "user-002";
//        System.out.println("\n=== 用户 B 开始对话 ===");
//        System.out.println("用户B: 我叫李四");
//        System.out.println("AI: " + aiCodeHelperService.chat(userBId, "我叫李四"));
//
//        System.out.println("\n用户B: 我叫什么名字？");
//        System.out.println("AI: " + aiCodeHelperService.chat(userBId, "我叫什么名字？"));
//        // 预期：AI 能回答出 "李四"
//
//        // 关键测试：用户 B 问 A 的名字，看会不会串话
//        System.out.println("\n=== 串话测试 ===");
//        System.out.println("用户B: 刚才那个用户叫什么名字？");
//        System.out.println("AI: " + aiCodeHelperService.chat(userBId, "刚才那个用户叫什么名字？"));
//        // 预期：AI 不知道或说不记得，因为 B 的记忆里没有 A 的信息 ✅
//    }
//
//    @Test
//    public void testRag() {
//        String result= aiCodeHelperService.chat("Mysql的锁有哪些");
//        System.out.println(result);
//    }

//    @Test
//    public void testMPC() {
//        String result= aiCodeHelperService.chat("广东技术师范大学2026年秋季什么时候开学");
//        System.out.println(result);
//    }
}