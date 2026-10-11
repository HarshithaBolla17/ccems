package com.ccems.controller;

import com.ccems.dto.Dtos.*;
import com.ccems.model.EventStatus;
import com.ccems.security.AuthUser;
import com.ccems.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService events;
    private final com.ccems.service.PaymentService payments;

    public EventController(EventService events, com.ccems.service.PaymentService payments) {
        this.events = events;
        this.payments = payments;
    }

    @GetMapping
    public PageResponse<EventDto> feed(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "9") int size,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) EventStatus status,
                                       @RequestParam(defaultValue = "false") boolean openOnly,
                                       @RequestParam(defaultValue = "eventDate") String sortBy,
                                       @RequestParam(defaultValue = "asc") String dir) {
        return events.feed(AuthUser.current(), page, size, q, status, openOnly, sortBy, dir);
    }

    @GetMapping("/{id}")
    public EventDto get(@PathVariable Long id) {
        return events.get(AuthUser.current(), id);
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public EventDto register(@PathVariable Long id) {
        return events.register(AuthUser.current(), id);
    }

    @DeleteMapping("/{id}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public EventDto cancel(@PathVariable Long id) {
        return events.cancelRegistration(AuthUser.current(), id);
    }

    /** Pays for a held seat on a paid event. */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('STUDENT')")
    public EventDto pay(@PathVariable Long id, @Valid @RequestBody PayRequest req) {
        return payments.pay(AuthUser.current(), id, req.method());
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasRole('STUDENT')")
    public ReceiptDto receipt(@PathVariable Long id) {
        return payments.receipt(AuthUser.current(), id);
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public EventDto feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest req) {
        return events.rate(AuthUser.current(), id, req.rating());
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public List<RegistrationDto> registrations(@PathVariable Long id) {
        return events.registrationsFor(AuthUser.current(), id);
    }

    /** Marks attendance. Every PRESENT mark triggers certificate issuance. */
    @PutMapping("/{id}/attendance")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public AttendanceResult attendance(@PathVariable Long id, @Valid @RequestBody AttendanceRequest req) {
        return events.markAttendance(AuthUser.current(), id, req);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public EventDto status(@PathVariable Long id, @Valid @RequestBody EventStatusRequest req) {
        return events.setStatus(AuthUser.current(), id, req.status());
    }
}
