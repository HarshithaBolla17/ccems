package com.ccems.dto;

import com.ccems.model.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** All request/response shapes. Entities (and password hashes) never leave the service layer. */
public final class Dtos {

    private Dtos() {}

    // ---------- requests ----------

    public record RegisterRequest(
            @NotBlank(message = "Full name is required") String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
            Role role,
            String branch,
            @Min(value = 1, message = "Year must be between 1 and 6") @Max(value = 6, message = "Year must be between 1 and 6") Integer year,
            String rollNo) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record ClubRequest(
            @NotBlank(message = "Club name is required") String name,
            @Size(max = 1000, message = "Description is too long") String description,
            String category,
            Long coordinatorId) {}

    public record AssignCoordinatorRequest(@NotNull(message = "Choose a coordinator") Long coordinatorId) {}

    public record ClubStatusRequest(@NotNull ClubStatus status) {}

    public record EventRequest(
            @NotBlank(message = "Title is required") String title,
            @Size(max = 2000, message = "Description is too long") String description,
            String venue,
            @NotNull(message = "Event date is required") LocalDateTime eventDate,
            @NotNull(message = "Registration deadline is required") LocalDateTime registrationDeadline,
            @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be at least 1") Integer capacity,
            @Min(value = 0, message = "Price can't be negative") @Max(value = 100000, message = "Price is too high") Integer price) {}

    public record PayRequest(@NotBlank(message = "Choose a payment method") String method) {}

    public record EventStatusRequest(@NotNull EventStatus status) {}

    public record AttendanceMark(@NotNull Long registrationId, @NotNull AttendanceStatus status) {}

    public record AttendanceRequest(@NotEmpty(message = "No attendance records sent") List<AttendanceMark> records) {}

    public record FeedbackRequest(
            @NotNull(message = "Rating is required") @Min(value = 1, message = "Rating must be 1 to 5") @Max(value = 5, message = "Rating must be 1 to 5") Integer rating) {}

    // ---------- responses ----------

    public record ApiError(String timestamp, int status, String message, Map<String, String> errors) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record UserDto(Long userId, String fullName, String email, Role role,
                          String branch, Integer year, String rollNo) {}

    public record AuthResponse(String token, UserDto user) {}

    public record ClubDto(Long clubId, String name, String description, String category, ClubStatus status,
                          Long coordinatorId, String coordinatorName, long memberCount,
                          long upcomingEvents, boolean joined) {}

    public record EventDto(Long eventId, Long clubId, String clubName, String clubCategory,
                           String title, String description, String venue,
                           LocalDateTime eventDate, LocalDateTime registrationDeadline,
                           int capacity, long registeredCount, long seatsLeft, EventStatus status,
                           boolean registrationOpen, String closedReason,
                           boolean registered, AttendanceStatus myAttendance, Integer myRating,
                           boolean memberOfClub, int price, PaymentStatus myPayment,
                           LocalDateTime holdExpiresAt) {}

    public record ReceiptDto(String receiptNo, String reference, String studentName, String rollNo,
                             String eventTitle, String clubName, LocalDateTime eventDate, int amount,
                             String method, PaymentStatus status, LocalDateTime paidAt,
                             LocalDateTime refundedAt) {}

    public record VerifyDto(boolean valid, String code, String studentName, String eventTitle,
                            String clubName, LocalDateTime eventDate, LocalDateTime issuedAt) {}

    public record RegistrationDto(Long registrationId, Long eventId, String eventTitle, String clubName,
                                  LocalDateTime eventDate, Long studentId, String studentName,
                                  String studentEmail, String rollNo, String branch, Integer year,
                                  LocalDateTime registeredAt, AttendanceStatus attendanceStatus,
                                  Integer feedbackRating, Long certificateId,
                                  PaymentStatus paymentStatus, int amountPaid) {}

    public record CertificateDto(Long certificateId, String code, Long studentId, String studentName,
                                 String rollNo, String branch, String eventTitle, String clubName,
                                 LocalDateTime eventDate, LocalDateTime issuedAt, String certificateUrl) {}

    public record AttendanceResult(int marked, int certificatesIssued, List<RegistrationDto> registrations) {}

    public record ProfileDto(UserDto user, List<ClubDto> clubs, List<RegistrationDto> registrations,
                             List<CertificateDto> certificates) {}

    public record ClubActivity(Long clubId, String clubName, String coordinatorName, long members,
                               long events, long registrations, long attended, long revenue) {}

    public record AdminStats(long students, long coordinators, long activeClubs, long totalClubs,
                             long upcomingEvents, long completedEvents, long registrations,
                             long attended, long certificates, long revenue,
                             List<ClubActivity> clubActivity, List<RegistrationDto> recentRegistrations) {}
}
