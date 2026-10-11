package com.ccems.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_mutes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_scope", "scope_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ChatMute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mute_id")
    private Long muteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_scope", nullable = false)
    private ChatScope scope;

    @Column(name = "scope_id", nullable = false)
    private Long scopeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "muted_by", nullable = false)
    private Long mutedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
