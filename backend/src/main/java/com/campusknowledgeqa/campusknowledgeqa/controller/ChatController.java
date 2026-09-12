package com.campusknowledgeqa.campusknowledgeqa.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // 调用大模型
        return chatClient.prompt(message)
                .call()
                .content();
    }
    @GetMapping("/chatByTemplate")
    public String chatByTemplate(@RequestParam String userQuestion) {
        String templateText = """
            你是校园知识库问答助手，回答用户校园相关问题。
            用户问题：{question}
            """;

        return chatClient.prompt()
                .system(templateText)
                .user(p -> p.param("question", userQuestion))
                .call()
                .content();
    }

}
