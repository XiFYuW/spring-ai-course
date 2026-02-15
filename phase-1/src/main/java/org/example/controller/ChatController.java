package org.example.controller;

import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Mono<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.message());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.message());
    }

    @DeleteMapping("/history")
    public Mono<Void> clearHistory() {
        chatService.clearHistory();
        return Mono.empty();
    }

    public record ChatRequest(String message) {}
}
