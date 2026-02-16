package org.example.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final List<Message> conversationHistory;

    public ChatService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.conversationHistory = new ArrayList<>();
        this.conversationHistory.add(new SystemMessage("你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"));
    }

    public Mono<String> chat(String userMessage) {
        conversationHistory.add(new UserMessage(userMessage));
        Prompt prompt = new Prompt(conversationHistory);
        
        return Mono.fromCallable(() -> 
                chatClient.prompt(prompt)
                        .call()
                        .content()
        )
        .subscribeOn(Schedulers.boundedElastic())  // 在弹性线程池中执行阻塞操作
        .doOnNext(response -> {
            if (response != null && !response.isEmpty()) {
                conversationHistory.add(new AssistantMessage(response));
            }
        });
    }

    public Flux<String> chatStream(String userMessage) {
        conversationHistory.add(new UserMessage(userMessage));
        Prompt prompt = new Prompt(conversationHistory);
        
        StringBuilder fullResponse = new StringBuilder();
        
        return chatClient.prompt(prompt)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    if (!fullResponse.isEmpty()) {
                        conversationHistory.add(new AssistantMessage(fullResponse.toString()));
                    }
                });
    }

    public void clearHistory() {
        conversationHistory.clear();
        conversationHistory.add(new SystemMessage("你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。"));
    }

    public List<Message> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
}
