package com.ccems.controller;

import com.ccems.dto.HubDtos.NotificationList;
import com.ccems.dto.HubDtos.Summary;
import com.ccems.security.AuthUser;
import com.ccems.service.ChatService;
import com.ccems.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final ChatService chat;

    public NotificationController(NotificationService notifications, ChatService chat) {
        this.notifications = notifications;
        this.chat = chat;
    }

    @GetMapping
    public NotificationList list() {
        return notifications.list(AuthUser.current());
    }

    /** Polled every few seconds by the navbar: unread bell items and unread chat messages. */
    @GetMapping("/summary")
    public Summary summary() {
        AuthUser u = AuthUser.current();
        return new Summary(notifications.unread(u.userId()), chat.unreadTotal(u));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) {
        notifications.markRead(AuthUser.current(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() {
        notifications.markAllRead(AuthUser.current());
    }
}
