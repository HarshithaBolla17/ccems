package com.ccems.service;

import com.ccems.dto.Dtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClubService {

    private final ClubRepository clubs;
    private final ClubMemberRepository members;
    private final UserRepository users;
    private final DtoMapper mapper;

    public ClubService(ClubRepository clubs, ClubMemberRepository members,
                       UserRepository users, DtoMapper mapper) {
        this.clubs = clubs;
        this.members = members;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClubDto> list(AuthUser caller, int page, int size, String q, boolean includeInactive) {
        // Only admins see inactive clubs.
        ClubStatus status = (includeInactive && caller.isAdmin()) ? null : ClubStatus.ACTIVE;
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Page<Club> result = clubs.search(status, query,
                PageRequest.of(Math.max(page, 0), clamp(size), Sort.by("name").ascending()));
        return new PageResponse<>(
                result.getContent().stream().map(c -> mapper.club(c, caller.userId())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ClubDto get(AuthUser caller, Long id) {
        return mapper.club(find(id), caller.userId());
    }

    /** Clubs the caller can run: their own for coordinators, everything for admins. */
    @Transactional(readOnly = true)
    public List<ClubDto> managed(AuthUser caller) {
        List<Club> list = caller.isAdmin()
                ? clubs.findAll(Sort.by("name"))
                : clubs.findByCoordinator_UserId(caller.userId());
        return list.stream().map(c -> mapper.club(c, caller.userId())).toList();
    }

    @Transactional
    public ClubDto create(AuthUser caller, ClubRequest req) {
        if (clubs.existsByNameIgnoreCase(req.name().trim())) {
            throw BusinessException.conflict("A club with this name already exists.");
        }
        Club c = new Club();
        c.setName(req.name().trim());
        c.setDescription(req.description());
        c.setCategory(blankToNull(req.category()));
        c.setStatus(ClubStatus.ACTIVE);
        if (req.coordinatorId() != null) {
            c.setCoordinator(requireCoordinator(req.coordinatorId()));
        }
        return mapper.club(clubs.save(c), caller.userId());
    }

    @Transactional
    public ClubDto assignCoordinator(AuthUser caller, Long clubId, Long coordinatorId) {
        Club c = find(clubId);
        c.setCoordinator(requireCoordinator(coordinatorId));
        return mapper.club(c, caller.userId());
    }

    @Transactional
    public ClubDto setStatus(AuthUser caller, Long clubId, ClubStatus status) {
        Club c = find(clubId);
        c.setStatus(status);
        return mapper.club(c, caller.userId());
    }

    @Transactional
    public ClubDto join(AuthUser caller, Long clubId) {
        Club c = find(clubId);
        if (c.getStatus() != ClubStatus.ACTIVE) {
            throw BusinessException.badRequest("This club is not accepting members right now.");
        }
        if (members.existsByClub_ClubIdAndStudent_UserId(clubId, caller.userId())) {
            throw BusinessException.conflict("You are already a member of " + c.getName() + ".");
        }
        ClubMember m = new ClubMember();
        m.setClub(c);
        m.setStudent(users.getReferenceById(caller.userId()));
        members.save(m);
        return mapper.club(c, caller.userId());
    }

    @Transactional
    public ClubDto leave(AuthUser caller, Long clubId) {
        Club c = find(clubId);
        ClubMember m = members.findByClub_ClubIdAndStudent_UserId(clubId, caller.userId())
                .orElseThrow(() -> BusinessException.badRequest("You are not a member of " + c.getName() + "."));
        members.delete(m);
        members.flush();
        return mapper.club(c, caller.userId());
    }

    /** Throws unless the caller is an admin or the coordinator of this club. */
    public void assertCanManage(AuthUser caller, Club club) {
        if (caller.isAdmin()) return;
        if (club.getCoordinator() == null || !club.getCoordinator().getUserId().equals(caller.userId())) {
            throw BusinessException.forbidden("You can only manage clubs assigned to you.");
        }
    }

    public Club find(Long id) {
        return clubs.findById(id).orElseThrow(() -> BusinessException.notFound("Club not found."));
    }

    private User requireCoordinator(Long id) {
        User u = users.findById(id).orElseThrow(() -> BusinessException.notFound("Coordinator not found."));
        if (u.getRole() != Role.COORDINATOR) {
            throw BusinessException.badRequest(u.getFullName() + " does not have a coordinator account.");
        }
        return u;
    }

    private int clamp(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
