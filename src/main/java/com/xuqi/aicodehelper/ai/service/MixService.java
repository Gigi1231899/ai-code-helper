package com.xuqi.aicodehelper.ai.service;


import com.xuqi.aicodehelper.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

@InputGuardrails(SafeInputGuardrail.class)
public interface MixService {

    @SystemMessage(fromResource= "mix-prompt.txt")
    Flux<String> chatMix(@MemoryId int memoryId, @UserMessage String message);
}
