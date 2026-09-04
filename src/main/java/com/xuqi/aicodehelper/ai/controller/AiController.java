package com.xuqi.aicodehelper.ai.controller;

import com.xuqi.aicodehelper.ai.service.McpService;
import com.xuqi.aicodehelper.ai.service.MixService;
import com.xuqi.aicodehelper.ai.service.RagService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {
    @Resource
    private RagService ragService;

    @Resource
    private McpService mcpService;
    @Resource
    private MixService mixService;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message,String mode){
        if(mode.equals("rag")){
            return ragService.chatRag(memoryId,message).
                    map(chunk-> ServerSentEvent.
                            <String>builder().data(chunk).build());
        }else if(mode.equals("mcp")){
            return mcpService.chatMcp(memoryId,message).
                    map(chunk-> ServerSentEvent.
                            <String>builder().data(chunk).build());
        }else{
            return mixService.chatMix(memoryId,message).
                    map(chunk-> ServerSentEvent.
                            <String>builder().data(chunk).build());
        }


    }
}
