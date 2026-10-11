package com.ccems.service;

import com.ccems.dto.Dtos.EventDto;
import com.ccems.dto.HubDtos.*;
import com.ccems.exception.BusinessException;
import com.ccems.model.*;
import com.ccems.repository.*;
import com.ccems.security.AuthUser;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/** Announcements, shared links and files, and hand-in tasks for one event. */
@Service
public class HubService {

    public record Download(Resource resource, String fileName, MediaType mediaType) {}

    private final EventRepository events;
    private final AnnouncementRepository announcements;
    private final EventResourceRepository resources;
    private final SubmissionTaskRepository tasks;
    private final SubmissionRepository submissions;
    private final EventRegistrationRepository registrations;
    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final AccessService access;
    private final FileStorageService storage;
    private final NotificationService notifications;
    private final DtoMapper mapper;

    public HubService(EventRepository events, AnnouncementRepository announcements,
                      EventResourceRepository resources, SubmissionTaskRepository tasks,
                      SubmissionRepository submissions, EventRegistrationRepository registrations,
                      StudentProfileRepository profiles, UserRepository users, AccessService access,
                      FileStorageService storage, NotificationService notifications, DtoMapper mapper) {
        this.events = events;
        this.announcements = announcements;
        this.resources = resources;
        this.tasks = tasks;
        this.submissions = submissions;
        this.registrations = registrations;
        this.profiles = profiles;
        this.users = users;
        this.access = access;
        this.storage = storage;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------- hub

    @Transactional(readOnly = true)
    public HubDto hub(AuthUser u, Long eventId) {
        Event e = event(eventId);
        access.requireViewEvent(u, e);
        boolean manage = access.canManageClub(u, e.getClub());
        EventDto eventDto = mapper.event(e, u.userId());
        return new HubDto(eventDto, manage,
                announcements.findByEvent_EventIdOrderByPinnedDescCreatedAtDesc(eventId).stream()
                        .map(this::announcementDto).toList(),
                resources.findByEvent_EventIdOrderByCreatedAtDesc(eventId).stream()
                        .map(this::resourceDto).toList(),
                tasks.findByEvent_EventIdOrderByDeadlineAsc(eventId).stream()
                        .map(t -> taskDto(t, u, manage)).toList());
    }

    // --------------------------------------------------------- announcements

    @Transactional
    public AnnouncementDto postAnnouncement(AuthUser u, Long eventId, AnnouncementRequest req) {
        Event e = manageable(u, eventId);
        Announcement a = new Announcement();
        a.setEvent(e);
        a.setTitle(req.title().trim());
        a.setBody(blankToNull(req.body()));
        a.setPinned(req.pinned());
        a.setUrgent(req.urgent());
        a.setAuthor(users.getReferenceById(u.userId()));
        announcements.save(a);

        notifications.notifyEventParticipants(e, NotificationType.ANNOUNCEMENT,
                (req.urgent() ? "Urgent: " : "") + e.getTitle(), req.title().trim(),
                "/events/" + eventId + "/hub", u.userId());
        return announcementDto(a);
    }

    @Transactional
    public void deleteAnnouncement(AuthUser u, Long id) {
        Announcement a = announcements.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Announcement not found."));
        access.requireManage(u, a.getEvent().getClub());
        announcements.delete(a);
    }

    @Transactional
    public AnnouncementDto togglePin(AuthUser u, Long id) {
        Announcement a = announcements.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Announcement not found."));
        access.requireManage(u, a.getEvent().getClub());
        a.setPinned(!a.isPinned());
        return announcementDto(a);
    }

    // -------------------------------------------------------------- resources

    @Transactional
    public ResourceDto addLink(AuthUser u, Long eventId, LinkRequest req) {
        Event e = manageable(u, eventId);
        EventResource r = new EventResource();
        r.setEvent(e);
        r.setTitle(req.title().trim());
        r.setDescription(blankToNull(req.description()));
        r.setResourceType(ResourceType.LINK);
        r.setLinkUrl(validUrl(req.url()));
        r.setUploadedBy(users.getReferenceById(u.userId()));
        resources.save(r);
        return resourceDto(r);
    }

    @Transactional
    public ResourceDto addFile(AuthUser u, Long eventId, String title, String description, MultipartFile file) {
        Event e = manageable(u, eventId);
        if (title == null || title.isBlank()) throw BusinessException.badRequest("Title is required.");
        if (title.length() > 150) throw BusinessException.badRequest("Title is too long.");
        FileStorageService.Stored stored = storage.store(file);

        EventResource r = new EventResource();
        r.setEvent(e);
        r.setTitle(title.trim());
        r.setDescription(blankToNull(description));
        r.setResourceType(ResourceType.FILE);
        r.setStoredName(stored.storedName());
        r.setOriginalName(stored.originalName());
        r.setFileSize(stored.size());
        r.setUploadedBy(users.getReferenceById(u.userId()));
        resources.save(r);
        return resourceDto(r);
    }

    @Transactional
    public void deleteResource(AuthUser u, Long id) {
        EventResource r = resources.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Resource not found."));
        access.requireManage(u, r.getEvent().getClub());
        storage.delete(r.getStoredName());
        resources.delete(r);
    }

    @Transactional(readOnly = true)
    public Download downloadResource(AuthUser u, Long id) {
        EventResource r = resources.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Resource not found."));
        access.requireViewEvent(u, r.getEvent());
        if (r.getResourceType() != ResourceType.FILE || r.getStoredName() == null) {
            throw BusinessException.badRequest("This resource is a link, not a file.");
        }
        return new Download(storage.load(r.getStoredName()), r.getOriginalName(),
                storage.mediaType(r.getOriginalName()));
    }

    // ------------------------------------------------------------------ tasks

    @Transactional
    public TaskDto createTask(AuthUser u, Long eventId, TaskRequest req) {
        Event e = manageable(u, eventId);
        if (!req.deadline().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("The deadline must be in the future.");
        }
        SubmissionTask t = new SubmissionTask();
        t.setEvent(e);
        t.setTitle(req.title().trim());
        t.setDescription(blankToNull(req.description()));
        t.setDeadline(req.deadline());
        t.setCreatedBy(users.getReferenceById(u.userId()));
        tasks.save(t);

        notifications.notifyEventParticipants(e, NotificationType.TASK,
                "New task: " + t.getTitle(), e.getTitle(),
                "/events/" + eventId + "/hub?tab=tasks", u.userId());
        return taskDto(t, u, true);
    }

    @Transactional
    public void deleteTask(AuthUser u, Long id) {
        SubmissionTask t = tasks.findById(id).orElseThrow(() -> BusinessException.notFound("Task not found."));
        access.requireManage(u, t.getEvent().getClub());
        for (Submission s : submissions.findByTask_TaskIdOrderBySubmittedAtAsc(id)) {
            storage.delete(s.getStoredName());
        }
        submissions.deleteAll(submissions.findByTask_TaskIdOrderBySubmittedAtAsc(id));
        tasks.delete(t);
    }

    /** A student hands in work. Handing in again before the deadline replaces the earlier version. */
    @Transactional
    public TaskDto submit(AuthUser u, Long taskId, String link, String note, MultipartFile file) {
        SubmissionTask t = tasks.findById(taskId).orElseThrow(() -> BusinessException.notFound("Task not found."));
        if (u.role() != Role.STUDENT) throw BusinessException.forbidden("Only students hand in work.");
        access.requireViewEvent(u, t.getEvent());
        if (!t.getDeadline().isAfter(LocalDateTime.now())) {
            throw BusinessException.badRequest("The deadline for this task has passed.");
        }
        boolean hasFile = file != null && !file.isEmpty();
        boolean hasLink = link != null && !link.isBlank();
        if (!hasFile && !hasLink) throw BusinessException.badRequest("Add a link or attach a file.");
        if (note != null && note.length() > 500) throw BusinessException.badRequest("The note is too long.");
        String cleanLink = hasLink ? validUrl(link) : null;
        FileStorageService.Stored stored = hasFile ? storage.store(file) : null;

        Submission s = submissions.findByTask_TaskIdAndStudent_UserId(taskId, u.userId()).orElseGet(() -> {
            Submission fresh = new Submission();
            fresh.setTask(t);
            fresh.setStudent(users.getReferenceById(u.userId()));
            return fresh;
        });
        storage.delete(s.getStoredName());
        s.setLinkUrl(cleanLink);
        s.setStoredName(stored == null ? null : stored.storedName());
        s.setOriginalName(stored == null ? null : stored.originalName());
        s.setFileSize(stored == null ? null : stored.size());
        s.setNote(blankToNull(note));
        s.setSubmittedAt(LocalDateTime.now());
        submissions.save(s);
        return taskDto(t, u, false);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> submissionsFor(AuthUser u, Long taskId) {
        SubmissionTask t = tasks.findById(taskId).orElseThrow(() -> BusinessException.notFound("Task not found."));
        access.requireManage(u, t.getEvent().getClub());
        return submissions.findByTask_TaskIdOrderBySubmittedAtAsc(taskId).stream()
                .map(this::submissionDto).toList();
    }

    @Transactional(readOnly = true)
    public Download downloadSubmission(AuthUser u, Long id) {
        Submission s = submissions.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Submission not found."));
        boolean owner = s.getStudent().getUserId().equals(u.userId());
        if (!owner) access.requireManage(u, s.getTask().getEvent().getClub());
        if (s.getStoredName() == null) throw BusinessException.badRequest("This submission has no file.");
        return new Download(storage.load(s.getStoredName()), s.getOriginalName(),
                storage.mediaType(s.getOriginalName()));
    }

    // --------------------------------------------------------------- mapping

    private AnnouncementDto announcementDto(Announcement a) {
        return new AnnouncementDto(a.getAnnouncementId(), a.getEvent().getEventId(), a.getTitle(), a.getBody(),
                a.isPinned(), a.isUrgent(), a.getAuthor() == null ? null : a.getAuthor().getFullName(),
                a.getCreatedAt());
    }

    private ResourceDto resourceDto(EventResource r) {
        return new ResourceDto(r.getResourceId(), r.getTitle(), r.getDescription(), r.getResourceType(),
                r.getLinkUrl(), r.getOriginalName(), r.getFileSize(),
                r.getUploadedBy() == null ? null : r.getUploadedBy().getFullName(), r.getCreatedAt());
    }

    private SubmissionDto submissionDto(Submission s) {
        User student = s.getStudent();
        String roll = profiles.findByUser_UserId(student.getUserId()).map(StudentProfile::getRollNo).orElse(null);
        return new SubmissionDto(s.getSubmissionId(), s.getTask().getTaskId(), student.getUserId(),
                student.getFullName(), roll, s.getLinkUrl(), s.getOriginalName(), s.getFileSize(),
                s.getNote(), s.getSubmittedAt());
    }

    private TaskDto taskDto(SubmissionTask t, AuthUser u, boolean staff) {
        long expected = registrations.findByEvent_EventIdOrderByRegisteredAtAsc(t.getEvent().getEventId())
                .stream().filter(r -> r.getPaymentStatus() != PaymentStatus.PENDING).count();
        SubmissionDto mine = null;
        if (!staff) {
            mine = submissions.findByTask_TaskIdAndStudent_UserId(t.getTaskId(), u.userId())
                    .map(this::submissionDto).orElse(null);
        }
        return new TaskDto(t.getTaskId(), t.getEvent().getEventId(), t.getTitle(), t.getDescription(),
                t.getDeadline(), t.getDeadline().isAfter(LocalDateTime.now()),
                submissions.countByTask_TaskId(t.getTaskId()), expected, mine);
    }

    // --------------------------------------------------------------- helpers

    private Event event(Long id) {
        return events.findById(id).orElseThrow(() -> BusinessException.notFound("Event not found."));
    }

    private Event manageable(AuthUser u, Long eventId) {
        Event e = event(eventId);
        access.requireManage(u, e.getClub());
        return e;
    }

    private static String validUrl(String url) {
        String v = url == null ? "" : url.trim();
        if (!(v.startsWith("https://") || v.startsWith("http://")) || v.length() > 500 || v.contains(" ")) {
            throw BusinessException.badRequest("Enter a full link that starts with http:// or https://");
        }
        return v;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
