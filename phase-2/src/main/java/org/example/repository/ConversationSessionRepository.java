package org.example.repository;

import org.example.entity.ConversationSession;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ConversationSessionRepository extends R2dbcRepository<ConversationSession, Long> {
    
    Flux<ConversationSession> findAllByOrderByUpdatedAtDesc();
}
