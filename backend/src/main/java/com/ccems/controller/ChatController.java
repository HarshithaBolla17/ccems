package com.ccems.controller;

import com.ccems.dto.HubDtos.*;
import com.ccems.security.AuthUser;
import com.ccems.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** scope is "event" or "club"; id is the event or club id. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/rooms")
    public List<RoomDto> rooms() {
        return chat.rooms(AuthUser.current());
    }

    @GetMapping("/{scope}/{id}/messages")
    public ChatDto messages(@PathVariable String scope, @PathVariable Long id) {
        return chat.messages(AuthUser.current(), scope, id);
    }

    @PostMapping("/{scope}/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@PathVariable String scope, @PathVariable Long id, @Valid @RequestBody ChatSend req) {
        return chat.send(AuthUser.current(), scope, id, req.body());
    }

    @PostMapping("/{scope}/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable String scope, @PathVariable Long id) {
        chat.markRead(AuthUser.current(), scope, id);
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long messageId) {
        chat.delete(AuthUser.current(), messageId);
    }

    @GetMapping("/{scope}/{id}/mutes")
    public List<MutedDto> muted(@PathVariable String scope, @PathVariable Long id) {
        return chat.muted(AuthUser.current(), scope, id);
    }

    @PostMapping("/{scope}/{id}/mutes/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mute(@PathVariable String scope, @PathVariable Long id, @PathVariable Long userId) {
        chat.mute(AuthUser.current(), scope, id, userId);
    }

    @DeleteMapping("/{scope}/{id}/mutes/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmute(@PathVariable String scope, @PathVariable Long id, @PathVariable Long userId) {
        chat.unmute(AuthUser.current(), scope, id, userId);
    }
}
