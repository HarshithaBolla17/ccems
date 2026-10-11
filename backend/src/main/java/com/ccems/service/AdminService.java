package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.model.*;
import com.ccems.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository users;
    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final CertificateRepository certificates;
    private final PaymentRepository payments;
    private final DtoMapper mapper;

    public AdminService(UserRepository users, ClubRepository clubs, ClubMemberRepository members,
                        EventRepository events, EventRegistrationRepository registrations,
                        CertificateRepository certificates, PaymentRepository payments, DtoMapper mapper) {
        this.payments = payments;
        this.users = users;
        this.clubs = clubs;
        this.members = members;
        this.events = events;
        this.registrations = registrations;
        this.certificates = certificates;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> coordinators() {
        return users.findByRole(Role.COORDINATOR).stream().map(mapper::user).toList();
    }

    /** Consolidated, campus-wide view: totals, per-club activity and the latest sign-ups. */
    @Transactional(readOnly = true)
    public AdminStats stats() {
        List<ClubActivity> activity = new ArrayList<>();
        long totalAttended = 0;

        for (Club c : clubs.findAll(Sort.by("name"))) {
            long eventCount = events.countByClub_ClubId(c.getClubId());
            long regs = 0;
            long present = 0;
            for (Event e : events.findByClub_ClubId(c.getClubId(), PageRequest.of(0, 1000)).getContent()) {
                regs += registrations.countByEvent_EventId(e.getEventId());
                present += registrations.countByEvent_EventIdAndAttendanceStatus(
                        e.getEventId(), AttendanceStatus.PRESENT);
            }
            totalAttended += present;
            activity.add(new ClubActivity(c.getClubId(), c.getName(),
                    c.getCoordinator() == null ? null : c.getCoordinator().getFullName(),
                    members.countByClub_ClubId(c.getClubId()), eventCount, regs, present,
                    payments.collectedByClub(c.getClubId())));
        }

        List<RegistrationDto> recent = registrations
                .findAll(PageRequest.of(0, 8, Sort.by("registeredAt").descending()))
                .getContent().stream().map(mapper::registration).toList();

        return new AdminStats(
                users.findByRole(Role.STUDENT).size(),
                users.findByRole(Role.COORDINATOR).size(),
                clubs.search(ClubStatus.ACTIVE, null, PageRequest.of(0, 1)).getTotalElements(),
                clubs.count(),
                events.countByStatus(EventStatus.UPCOMING),
                events.countByStatus(EventStatus.COMPLETED),
                registrations.count(),
                totalAttended,
                certificates.count(),
                payments.totalCollected(),
                activity, recent);
    }
}
