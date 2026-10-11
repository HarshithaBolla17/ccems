package com.ccems.controller;

import com.ccems.dto.HubDtos.*;
import com.ccems.security.AuthUser;
import com.ccems.service.HubService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HubController {

    private final HubService hub;

    public HubController(HubService hub) {
        this.hub = hub;
    }

    @GetMapping("/events/{id}/hub")
    public HubDto hub(@PathVariable Long id) {
        return hub.hub(AuthUser.current(), id);
    }

    // ---- announcements

    @PostMapping("/events/{id}/announcements")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementDto announce(@PathVariable Long id, @Valid @RequestBody AnnouncementRequest req) {
        return hub.postAnnouncement(AuthUser.current(), id, req);
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnouncement(@PathVariable Long id) {
        hub.deleteAnnouncement(AuthUser.current(), id);
    }

    @PutMapping("/announcements/{id}/pin")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public AnnouncementDto togglePin(@PathVariable Long id) {
        return hub.togglePin(AuthUser.current(), id);
    }

    // ---- resources

    @PostMapping("/events/{id}/resources/link")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto addLink(@PathVariable Long id, @Valid @RequestBody LinkRequest req) {
        return hub.addLink(AuthUser.current(), id, req);
    }

    @PostMapping(value = "/events/{id}/resources/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto addFile(@PathVariable Long id,
                               @RequestParam("title") String title,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam("file") MultipartFile file) {
        return hub.addFile(AuthUser.current(), id, title, description, file);
    }

    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@PathVariable Long id) {
        hub.deleteResource(AuthUser.current(), id);
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(@PathVariable Long id) {
        return file(hub.downloadResource(AuthUser.current(), id));
    }

    // ---- tasks and submissions

    @PostMapping("/events/{id}/tasks")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        return hub.createTask(AuthUser.current(), id, req);
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        hub.deleteTask(AuthUser.current(), id);
    }

    @PostMapping(value = "/tasks/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public TaskDto submit(@PathVariable Long id,
                          @RequestParam(value = "link", required = false) String link,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "file", required = false) MultipartFile file) {
        return hub.submit(AuthUser.current(), id, link, note, file);
    }

    @GetMapping("/tasks/{id}/submissions")
    @PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
    public List<SubmissionDto> submissions(@PathVariable Long id) {
        return hub.submissionsFor(AuthUser.current(), id);
    }

    @GetMapping("/submissions/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSubmission(@PathVariable Long id) {
        return file(hub.downloadSubmission(AuthUser.current(), id));
    }

    private ResponseEntity<org.springframework.core.io.Resource> file(HubService.Download d) {
        return ResponseEntity.ok()
                .contentType(d.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(d.resource());
    }
}
