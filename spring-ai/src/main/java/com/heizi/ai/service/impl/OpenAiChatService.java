package com.heizi.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.heizi.ai.service.ChatService;
import com.heizi.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAiChatService implements ChatService {

    private final ChatClient chatClient;

    public OpenAiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message) {
        if (StrUtil.isBlank(message)) {
            log.warn("AI 对话请求参数为空");
            throw new BusinessException("message 不能为空");
        }

        log.info("开始调用 AI 对话，messageLength={}", message.length());

        String content = chatClient.prompt()
                .user(message)
                .advisors(new SimpleLoggerAdvisor())
                .call()
                .content();

        log.info("AI 对话调用完成，responseLength={}", content == null ? 0 : content.length());
        return content;
    }

}
