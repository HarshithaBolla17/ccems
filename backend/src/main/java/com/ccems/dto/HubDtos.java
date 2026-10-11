package com.ccems.dto;

import com.ccems.dto.Dtos.CertificateDto;
import com.ccems.dto.Dtos.ClubDto;
import com.ccems.dto.Dtos.EventDto;
import com.ccems.model.NotificationType;
import com.ccems.model.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Shapes for the Event Hub, chat, notifications and the role home pages. */
public final class HubDtos {

    private HubDtos() {}

    // ---------- requests ----------

    public record AnnouncementRequest(
            @NotBlank(message = "Title is required") @Size(max = 150, message = "Title is too long") String title,
            @Size(max = 2000, message = "Message is too long") String body,
            boolean pinned,
            boolean urgent) {}

    public record LinkRequest(
            @NotBlank(message = "Title is required") @Size(max = 150, message = "Title is too long") String title,
            @NotBlank(message = "Link is required") @Size(max = 500, message = "Link is too long") String url,
            @Size(max = 300, message = "Description is too long") String description) {}

    public record TaskRequest(
            @NotBlank(message = "Title is required") @Size(max = 150, message = "Title is too long") String title,
            @Size(max = 2000, message = "Description is too long") String description,
            @NotNull(message = "Deadline is required") LocalDateTime deadline) {}

    public record ChatSend(
            @NotBlank(message = "Write a message first") @Size(max = 1000, message = "Messages can be up to 1000 characters") String body) {}

    // ---------- event hub ----------

    public record AnnouncementDto(Long announcementId, Long eventId, String title, String body,
                                  boolean pinned, boolean urgent, String authorName, LocalDateTime createdAt) {}

    public record ResourceDto(Long resourceId, String title, String description, ResourceType type,
                              String url, String fileName, Long fileSize, String uploadedBy,
                              LocalDateTime createdAt) {}

    public record SubmissionDto(Long submissionId, Long taskId, Long studentId, String studentName,
                                String rollNo, String linkUrl, String fileName, Long fileSize,
                                String note, LocalDateTime submittedAt) {}

    public record TaskDto(Long taskId, Long eventId, String title, String description,
                          LocalDateTime deadline, boolean open, long submittedCount, long expectedCount,
                          SubmissionDto mine) {}

    public record HubDto(EventDto event, boolean canManage, List<AnnouncementDto> announcements,
                         List<ResourceDto> resources, List<TaskDto> tasks) {}

    // ---------- chat ----------

    public record MessageDto(Long messageId, Long senderId, String senderName, String senderRole,
                             String body, LocalDateTime createdAt, boolean removed, boolean mine) {}

    public record MutedDto(Long userId, String name) {}

    public record ChatDto(String title, List<MessageDto> messages, boolean closed, String closedReason,
                          boolean muted, boolean canModerate) {}

    public record RoomDto(String scope, Long id, String title, String subtitle, long unread,
                          String lastMessage, String lastSender, LocalDateTime lastAt, boolean closed) {}

    // ---------- notifications ----------

    public record NotificationDto(Long notificationId, NotificationType type, String title, String body,
                                  String link, LocalDateTime createdAt, boolean read) {}

    public record NotificationList(List<NotificationDto> items, long unread) {}

    public record Summary(long unreadNotifications, long unreadChats) {}

    // ---------- home ----------

    public record AttentionItem(String kind, String title, String detail, String link, String tone,
                                LocalDateTime until) {}

    public record Stat(String label, String value, String hint) {}

    public record HomeDto(String role, String name, EventDto nextEvent, List<AttentionItem> attention,
                          List<EventDto> suggestedEvents, List<ClubDto> suggestedClubs,
                          List<ClubDto> managedClubs, List<Stat> stats, CertificateDto latestCertificate,
                          long unreadChats, long unreadNotifications) {}
}
