package com.ccems.controller;

import com.ccems.dto.Dtos.*;
import com.ccems.security.AuthUser;
import com.ccems.service.ClubService;
import com.ccems.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubs;
    private final EventService events;

    public ClubController(ClubService clubs, EventService events) {
        this.clubs = clubs;
        this.events = events;
    }

    @GetMapping
    public PageResponse<ClubDto> list(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "false") boolean includeInactive) {
        return clubs.list(AuthUser.current(), page, size, q, includeInactive);
    }

    @GetMapping("/managed")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public List<ClubDto> managed() {
        return clubs.managed(AuthUser.current());
    }

    @GetMapping("/{id}")
    public ClubDto get(@PathVariable Long id) {
        return clubs.get(AuthUser.current(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubDto create(@Valid @RequestBody ClubRequest req) {
        return clubs.create(AuthUser.current(), req);
    }

    @PutMapping("/{id}/coordinator")
    @PreAuthorize("hasRole('ADMIN')")
    public ClubDto assignCoordinator(@PathVariable Long id, @Valid @RequestBody AssignCoordinatorRequest req) {
        return clubs.assignCoordinator(AuthUser.current(), id, req.coordinatorId());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ClubDto setStatus(@PathVariable Long id, @Valid @RequestBody ClubStatusRequest req) {
        return clubs.setStatus(AuthUser.current(), id, req.status());
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ClubDto join(@PathVariable Long id) {
        return clubs.join(AuthUser.current(), id);
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public ClubDto leave(@PathVariable Long id) {
        return clubs.leave(AuthUser.current(), id);
    }

    @GetMapping("/{id}/events")
    public PageResponse<EventDto> events(@PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "eventDate") String sortBy,
                                         @RequestParam(defaultValue = "desc") String dir) {
        return events.forClub(AuthUser.current(), id, page, size, sortBy, dir);
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@PathVariable Long id, @Valid @RequestBody EventRequest req) {
        return events.create(AuthUser.current(), id, req);
    }
}
