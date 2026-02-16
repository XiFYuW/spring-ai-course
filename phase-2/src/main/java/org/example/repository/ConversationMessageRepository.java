package org.example.repository;

import org.example.entity.ConversationMessage;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ConversationMessageRepository extends R2dbcRepository<ConversationMessage, Long> {
    
    Flux<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    Flux<ConversationMessage> deleteBySessionId(Long sessionId);
}
