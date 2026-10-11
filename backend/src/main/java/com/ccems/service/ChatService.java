package com.ccems.service;

import com.ccems.dto.HubDtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Event chats (registered students + coordinator) and club chats (members + coordinator).
 * The browser polls every few seconds, so there is nothing extra to run or proxy.
 */
@Service
public class ChatService {

    static final int PAGE = 80;
    static final int GRACE_DAYS = 2;
    static final int MAX_PER_10_SECONDS = 8;

    private final ChatMessageRepository messages;
    private final ChatMuteRepository mutes;
    private final ChatReadRepository reads;
    private final EventRepository events;
    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final EventRegistrationRepository registrations;
    private final UserRepository users;
    private final AccessService access;

    public ChatService(ChatMessageRepository messages, ChatMuteRepository mutes, ChatReadRepository reads,
                       EventRepository events, ClubRepository clubs, ClubMemberRepository members,
                       EventRegistrationRepository registrations, UserRepository users, AccessService access) {
        this.messages = messages;
        this.mutes = mutes;
        this.reads = reads;
        this.events = events;
        this.clubs = clubs;
        this.members = members;
        this.registrations = registrations;
        this.users = users;
        this.access = access;
    }

    /** What the caller may do in one chat room. */
    private record Room(ChatScope scope, Long id, String title, boolean canView, boolean canModerate,
                        boolean closed, String closedReason) {}

    private Room room(AuthUser u, ChatScope scope, Long id) {
        if (scope == ChatScope.EVENT) {
            Event e = events.findById(id).orElseThrow(() -> BusinessException.notFound("Event not found."));
            boolean closed = LocalDateTime.now().isAfter(e.getEventDate().plusDays(GRACE_DAYS));
            return new Room(scope, id, e.getTitle(), access.canViewEvent(u, e),
                    access.canManageClub(u, e.getClub()), closed,
                    closed ? "This chat closed two days after the event. You can still read it." : null);
        }
        Club c = clubs.findById(id).orElseThrow(() -> BusinessException.notFound("Club not found."));
        boolean closed = c.getStatus() != ClubStatus.ACTIVE;
        return new Room(scope, id, c.getName(), access.canViewClub(u, c), access.canManageClub(u, c), closed,
                closed ? "This club is inactive, so its chat is read-only." : null);
    }

    private Room requireView(AuthUser u, ChatScope scope, Long id) {
        Room r = room(u, scope, id);
        if (!r.canView()) {
            throw BusinessException.forbidden(scope == ChatScope.EVENT
                    ? "This chat is open to students registered for the event and the club's coordinator."
                    : "This chat is open to the club's members and its coordinator.");
        }
        return r;
    }

    // --------------------------------------------------------------- messages

    @Transactional(readOnly = true)
    public ChatDto messages(AuthUser u, String scopeText, Long id) {
        ChatScope scope = ChatScope.parse(scopeText);
        Room r = requireView(u, scope, id);
        List<ChatMessage> page = new ArrayList<>(messages.latest(scope, id, PageRequest.of(0, PAGE)));
        Collections.reverse(page);
        boolean muted = mutes.find(scope, id, u.userId()).isPresent();
        return new ChatDto(r.title(), page.stream().map(m -> dto(m, u)).toList(), r.closed(),
                r.closedReason(), muted, r.canModerate());
    }

    @Transactional
    public MessageDto send(AuthUser u, String scopeText, Long id, String body) {
        ChatScope scope = ChatScope.parse(scopeText);
        Room r = requireView(u, scope, id);
        if (r.closed()) throw BusinessException.badRequest(r.closedReason());
        if (mutes.find(scope, id, u.userId()).isPresent()) {
            throw BusinessException.forbidden("A coordinator has muted you in this chat.");
        }
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) throw BusinessException.badRequest("Write a message first.");
        if (text.length() > 1000) throw BusinessException.badRequest("Messages can be up to 1000 characters.");
        long recent = messages.countBySender_UserIdAndCreatedAtAfter(u.userId(), LocalDateTime.now().minusSeconds(10));
        if (recent >= MAX_PER_10_SECONDS) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "You're sending messages too fast. Wait a few seconds.");
        }

        ChatMessage m = new ChatMessage();
        m.setScope(scope);
        m.setScopeId(id);
        m.setSender(users.getReferenceById(u.userId()));
        m.setBody(text);
        messages.save(m);
        return dto(m, u);
    }

    /** The sender can remove their own message; moderators can remove anyone's. */
    @Transactional
    public void delete(AuthUser u, Long messageId) {
        ChatMessage m = messages.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found."));
        boolean mine = m.getSender().getUserId().equals(u.userId());
        if (!mine && !room(u, m.getScope(), m.getScopeId()).canModerate()) {
            throw BusinessException.forbidden("You can only delete your own messages.");
        }
        m.setRemoved(true);
    }

    // ------------------------------------------------------------- moderation

    @Transactional
    public void mute(AuthUser u, String scopeText, Long id, Long targetUserId) {
        ChatScope scope = ChatScope.parse(scopeText);
        Room r = requireView(u, scope, id);
        if (!r.canModerate()) throw BusinessException.forbidden("Only the coordinator can mute students.");
        User target = users.findById(targetUserId).orElseThrow(() -> BusinessException.notFound("Student not found."));
        if (target.getRole() != Role.STUDENT) throw BusinessException.badRequest("Only students can be muted.");
        if (mutes.find(scope, id, targetUserId).isEmpty()) {
            ChatMute mute = new ChatMute();
            mute.setScope(scope);
            mute.setScopeId(id);
            mute.setUserId(targetUserId);
            mute.setMutedBy(u.userId());
            mutes.save(mute);
        }
    }

    @Transactional
    public void unmute(AuthUser u, String scopeText, Long id, Long targetUserId) {
        ChatScope scope = ChatScope.parse(scopeText);
        Room r = requireView(u, scope, id);
        if (!r.canModerate()) throw BusinessException.forbidden("Only the coordinator can unmute students.");
        mutes.find(scope, id, targetUserId).ifPresent(mutes::delete);
    }

    @Transactional(readOnly = true)
    public List<MutedDto> muted(AuthUser u, String scopeText, Long id) {
        ChatScope scope = ChatScope.parse(scopeText);
        Room r = requireView(u, scope, id);
        if (!r.canModerate()) throw BusinessException.forbidden("Only the coordinator can see muted students.");
        return mutes.findForRoom(scope, id).stream()
                .map(m -> users.findById(m.getUserId()).map(x -> new MutedDto(x.getUserId(), x.getFullName())).orElse(null))
                .filter(Objects::nonNull).toList();
    }

    // ------------------------------------------------------------- read state

    @Transactional
    public void markRead(AuthUser u, String scopeText, Long id) {
        ChatScope scope = ChatScope.parse(scopeText);
        requireView(u, scope, id);
        Long last = messages.lastId(scope, id);
        ChatRead read = reads.find(u.userId(), scope, id).orElseGet(() -> {
            ChatRead fresh = new ChatRead();
            fresh.setUserId(u.userId());
            fresh.setScope(scope);
            fresh.setScopeId(id);
            return fresh;
        });
        read.setLastMessageId(last == null ? 0L : last);
        reads.save(read);
    }

    /** Every chat the caller can open, with unread counts, most recently active first. */
    @Transactional(readOnly = true)
    public List<RoomDto> rooms(AuthUser u) {
        LocalDateTime now = LocalDateTime.now();
        List<RoomDto> out = new ArrayList<>();
        Set<Long> clubIds = new LinkedHashSet<>();
        Map<Long, Event> eventMap = new LinkedHashMap<>();

        if (u.role() == Role.STUDENT) {
            for (ClubMember m : members.findByStudent_UserIdOrderByJoinedAtDesc(u.userId())) {
                if (m.getClub().getStatus() == ClubStatus.ACTIVE) clubIds.add(m.getClub().getClubId());
            }
            for (EventRegistration r : registrations.findByStudent_UserIdOrderByRegisteredAtDesc(u.userId())) {
                if (r.getPaymentStatus() != PaymentStatus.PENDING) eventMap.put(r.getEvent().getEventId(), r.getEvent());
            }
        } else {
            List<Club> mine = u.isAdmin() ? clubs.findAll() : clubs.findByCoordinator_UserId(u.userId());
            for (Club c : mine) {
                if (c.getStatus() != ClubStatus.ACTIVE) continue;
                clubIds.add(c.getClubId());
                for (Event e : events.findByClub_ClubId(c.getClubId(), PageRequest.of(0, 100)).getContent()) {
                    eventMap.put(e.getEventId(), e);
                }
            }
        }

        for (Long cid : clubIds) {
            Club c = clubs.findById(cid).orElse(null);
            if (c != null) out.add(roomDto(u, ChatScope.CLUB, cid, c.getName(), "Club chat", false));
        }
        for (Event e : eventMap.values()) {
            // Staff only see recent events, so old ones do not pile up.
            if (u.role() != Role.STUDENT && e.getEventDate().isBefore(now.minusDays(14))) continue;
            boolean closed = now.isAfter(e.getEventDate().plusDays(GRACE_DAYS));
            out.add(roomDto(u, ChatScope.EVENT, e.getEventId(), e.getTitle(), e.getClub().getName(), closed));
        }

        out.sort(Comparator
                .comparing((RoomDto r) -> r.lastAt() == null ? LocalDateTime.MIN : r.lastAt()).reversed()
                .thenComparing(RoomDto::title));
        return out;
    }

    @Transactional(readOnly = true)
    public long unreadTotal(AuthUser u) {
        return rooms(u).stream().mapToLong(RoomDto::unread).sum();
    }

    // ---------------------------------------------------------------- helpers

    private RoomDto roomDto(AuthUser u, ChatScope scope, Long id, String title, String subtitle, boolean closed) {
        Long lastRead = reads.find(u.userId(), scope, id).map(ChatRead::getLastMessageId).orElse(0L);
        long unread = messages.countUnread(scope, id, lastRead, u.userId());
        List<ChatMessage> last = messages.latest(scope, id, PageRequest.of(0, 1));
        String preview = null;
        String who = null;
        LocalDateTime at = null;
        if (!last.isEmpty()) {
            ChatMessage m = last.get(0);
            preview = m.isRemoved() ? "Message deleted" : m.getBody();
            who = m.getSender().getFullName();
            at = m.getCreatedAt();
        }
        return new RoomDto(scope.name().toLowerCase(), id, title, subtitle, unread, preview, who, at, closed);
    }

    private MessageDto dto(ChatMessage m, AuthUser viewer) {
        User s = m.getSender();
        return new MessageDto(m.getMessageId(), s.getUserId(), s.getFullName(), s.getRole().name(),
                m.isRemoved() ? null : m.getBody(), m.getCreatedAt(), m.isRemoved(),
                s.getUserId().equals(viewer.userId()));
    }
}
