package com.ccems.service;

import com.ccems.dto.HubDtos.NotificationDto;
import com.ccems.dto.HubDtos.NotificationList;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.ClubMemberRepository;
import com.ccems.repository.EventRegistrationRepository;
import com.ccems.repository.NotificationRepository;
import com.ccems.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** The bell. Other services call notify() when something a student cares about happens. */
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final EventRegistrationRepository registrations;
    private final ClubMemberRepository members;

    public NotificationService(NotificationRepository repo, EventRegistrationRepository registrations,
                               ClubMemberRepository members) {
        this.repo = repo;
        this.registrations = registrations;
        this.members = members;
    }

    @Transactional
    public void notify(Long userId, NotificationType type, String title, String body, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(cut(title, 150));
        n.setBody(cut(body, 300));
        n.setLinkUrl(link);
        repo.save(n);
    }

    /** Everyone who can open the event hub (registered, and paid if the event is paid). */
    @Transactional
    public void notifyEventParticipants(Event e, NotificationType type, String title, String body,
                                        String link, Long exceptUserId) {
        for (EventRegistration r : registrations.findByEvent_EventIdOrderByRegisteredAtAsc(e.getEventId())) {
            Long uid = r.getStudent().getUserId();
            if (r.getPaymentStatus() == PaymentStatus.PENDING || uid.equals(exceptUserId)) continue;
            notify(uid, type, title, body, link);
        }
    }

    @Transactional
    public void notifyClubMembers(Club club, NotificationType type, String title, String body,
                                  String link, Long exceptUserId) {
        for (ClubMember m : members.findByClub_ClubId(club.getClubId())) {
            Long uid = m.getStudent().getUserId();
            if (!uid.equals(exceptUserId)) notify(uid, type, title, body, link);
        }
    }

    @Transactional(readOnly = true)
    public NotificationList list(AuthUser u) {
        List<NotificationDto> items = repo.findByUserIdOrderByCreatedAtDesc(u.userId(), PageRequest.of(0, 30))
                .stream()
                .map(n -> new NotificationDto(n.getNotificationId(), n.getType(), n.getTitle(), n.getBody(),
                        n.getLinkUrl(), n.getCreatedAt(), n.getReadAt() != null))
                .toList();
        return new NotificationList(items, repo.countByUserIdAndReadAtIsNull(u.userId()));
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(AuthUser u, Long id) {
        Notification n = repo.findByNotificationIdAndUserId(id, u.userId())
                .orElseThrow(() -> BusinessException.notFound("Notification not found."));
        if (n.getReadAt() == null) n.setReadAt(LocalDateTime.now());
    }

    @Transactional
    public void markAllRead(AuthUser u) {
        repo.markAllRead(u.userId(), LocalDateTime.now());
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
