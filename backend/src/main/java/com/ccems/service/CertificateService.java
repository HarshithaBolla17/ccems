package com.ccems.service;

import com.ccems.dto.Dtos.CertificateDto;
import com.ccems.dto.Dtos.VerifyDto;
import com.ccems.exception.BusinessException;
import com.ccems.model.Certificate;
import com.ccems.model.EventRegistration;
import com.ccems.repository.CertificateRepository;
import com.ccems.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CertificateService {

    private final CertificateRepository certificates;
    private final ClubService clubService;
    private final DtoMapper mapper;
    private final NotificationService notifications;

    public CertificateService(CertificateRepository certificates, ClubService clubService, DtoMapper mapper,
                              NotificationService notifications) {
        this.notifications = notifications;
        this.certificates = certificates;
        this.clubService = clubService;
        this.mapper = mapper;
    }

    /** Creates the certificate for a PRESENT registration. Returns true if a new one was issued. */
    @Transactional
    public boolean issueIfMissing(EventRegistration reg) {
        if (certificates.findByRegistration_RegistrationId(reg.getRegistrationId()).isPresent()) {
            return false;
        }
        Certificate c = new Certificate();
        c.setRegistration(reg);
        certificates.save(c);
        c.setCertificateUrl("/certificate/" + c.getCertificateId());
        notifications.notify(reg.getStudent().getUserId(), com.ccems.model.NotificationType.CERTIFICATE,
                "Your certificate is ready", reg.getEvent().getTitle(), "/certificate/" + c.getCertificateId());
        return true;
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> forStudent(AuthUser caller, Long studentId) {
        if (caller.role() == com.ccems.model.Role.STUDENT && !caller.userId().equals(studentId)) {
            throw BusinessException.forbidden("You can only view your own certificates.");
        }
        return certificates.findByRegistration_Student_UserIdOrderByIssuedAtDesc(studentId)
                .stream().map(mapper::certificate).toList();
    }

    /** Public check: is this code a real certificate issued by CCEMS? */
    @Transactional(readOnly = true)
    public VerifyDto verify(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        VerifyDto invalid = new VerifyDto(false, code, null, null, null, null, null);
        String[] parts = code.split("-");
        if (parts.length != 3 || !parts[2].matches("\\d{1,9}")) return invalid;

        return certificates.findById(Long.parseLong(parts[2]))
                .filter(c -> DtoMapper.code(c).equals(code))
                .map(c -> new VerifyDto(true, code,
                        c.getRegistration().getStudent().getFullName(),
                        c.getRegistration().getEvent().getTitle(),
                        c.getRegistration().getEvent().getClub().getName(),
                        c.getRegistration().getEvent().getEventDate(),
                        c.getIssuedAt()))
                .orElse(invalid);
    }

    @Transactional(readOnly = true)
    public CertificateDto get(AuthUser caller, Long id) {
        Certificate c = certificates.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Certificate not found."));
        Long ownerId = c.getRegistration().getStudent().getUserId();
        boolean owner = ownerId.equals(caller.userId());
        boolean staff = false;
        if (!owner) {
            if (caller.isAdmin()) {
                staff = true;
            } else if (caller.role() == com.ccems.model.Role.COORDINATOR) {
                var coord = c.getRegistration().getEvent().getClub().getCoordinator();
                staff = coord != null && coord.getUserId().equals(caller.userId());
            }
        }
        if (!owner && !staff) {
            throw BusinessException.forbidden("You can't view this certificate.");
        }
        return mapper.certificate(c);
    }
}
