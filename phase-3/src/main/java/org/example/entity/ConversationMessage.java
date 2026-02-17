package org.example.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("conversation_messages")
public record ConversationMessage(
    @Id
    Long id,
    
    @Column("session_id")
    Long sessionId,
    
    @Column("role")
    String role,
    
    @Column("content")
    String content,
    
    @Column("created_at")
    LocalDateTime createdAt
) {
    public static ConversationMessage of(Long sessionId, String role, String content) {
        return new ConversationMessage(
            null,
            sessionId,
            role,
            content,
            LocalDateTime.now()
        );
    }
}
