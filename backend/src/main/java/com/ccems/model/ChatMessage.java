package com.ccems.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One chat message in an event chat or a club chat. */
@Entity
@Table(name = "chat_messages",
        indexes = @Index(name = "idx_chat_room", columnList = "chat_scope, scope_id, message_id"))
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_scope", nullable = false)
    private ChatScope scope;

    /** The event id or club id, depending on the scope. */
    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Deleted messages stay in the table but show as removed. */
    @Column(nullable = false)
    private boolean removed;
}
