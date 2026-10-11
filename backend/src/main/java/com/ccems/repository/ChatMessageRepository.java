package com.ccems.repository;

import com.ccems.model.ChatMessage;
import com.ccems.model.ChatScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Newest first. The caller reverses the page to show oldest first. */
    @Query("select m from ChatMessage m where m.scope = :scope and m.scopeId = :sid order by m.messageId desc")
    List<ChatMessage> latest(@Param("scope") ChatScope scope, @Param("sid") Long sid, Pageable pageable);

    @Query("""
            select count(m) from ChatMessage m
            where m.scope = :scope and m.scopeId = :sid and m.messageId > :after
              and m.sender.userId <> :uid and m.removed = false
            """)
    long countUnread(@Param("scope") ChatScope scope, @Param("sid") Long sid,
                     @Param("after") Long after, @Param("uid") Long uid);

    @Query("select max(m.messageId) from ChatMessage m where m.scope = :scope and m.scopeId = :sid")
    Long lastId(@Param("scope") ChatScope scope, @Param("sid") Long sid);

    long countBySender_UserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
