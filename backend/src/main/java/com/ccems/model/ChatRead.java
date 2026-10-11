package com.ccems.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** How far one user has read in one chat. Used for unread counts. */
@Entity
@Table(name = "chat_reads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chat_scope", "scope_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ChatRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_id")
    private Long readId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_scope", nullable = false)
    private ChatScope scope;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "last_message_id", nullable = false)
    private Long lastMessageId = 0L;
}
