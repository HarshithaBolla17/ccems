package com.ccems.service;

import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.ClubMemberRepository;
import com.ccems.repository.EventRegistrationRepository;
import com.ccems.security.AuthUser;
import org.springframework.stereotype.Service;

/** One place for "who may see or manage this?". Used by the hub, chat and uploads. */
@Service
public class AccessService {

    private final EventRegistrationRepository registrations;
    private final ClubMemberRepository members;

    public AccessService(EventRegistrationRepository registrations, ClubMemberRepository members) {
        this.registrations = registrations;
        this.members = members;
    }

    public boolean canManageClub(AuthUser u, Club club) {
        if (u.isAdmin()) return true;
        return u.role() == Role.COORDINATOR
                && club.getCoordinator() != null
                && club.getCoordinator().getUserId().equals(u.userId());
    }

    /** Staff of the club, or a student who is registered and has paid (if the event is paid). */
    public boolean canViewEvent(AuthUser u, Event e) {
        if (canManageClub(u, e.getClub())) return true;
        if (u.role() != Role.STUDENT) return false;
        return registrations.findByEvent_EventIdAndStudent_UserId(e.getEventId(), u.userId())
                .map(r -> r.getPaymentStatus() != PaymentStatus.PENDING)
                .orElse(false);
    }

    public boolean canViewClub(AuthUser u, Club club) {
        if (canManageClub(u, club)) return true;
        return u.role() == Role.STUDENT
                && members.existsByClub_ClubIdAndStudent_UserId(club.getClubId(), u.userId());
    }

    public void requireManage(AuthUser u, Club club) {
        if (!canManageClub(u, club)) {
            throw BusinessException.forbidden("Only the club's coordinator can do that.");
        }
    }

    public void requireViewEvent(AuthUser u, Event e) {
        if (!canViewEvent(u, e)) {
            throw BusinessException.forbidden("The event hub is open to registered students and the club's coordinator.");
        }
    }
}
