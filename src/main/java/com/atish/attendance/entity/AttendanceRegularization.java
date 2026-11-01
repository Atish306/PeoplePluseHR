package com.atish.attendance.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "attendance_regularizations")
public class AttendanceRegularization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private long totalDays;

    /**
     * Manager can approve up to 7 days.
     */
    @Column(nullable = false)
    private long managerApprovedDays = 0;

    /**
     * If totalDays > 7 (e.g. 9 days), remaining days (e.g. 2 days) must be approved by Admin.
     */
    @Column(nullable = false)
    private long adminApprovedDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegularizationStatus status = RegularizationStatus.PENDING_MANAGER;

    @Column(nullable = false)
    private LocalTime checkInTime = LocalTime.of(9, 0);

    @Column(nullable = false)
    private LocalTime checkOutTime = LocalTime.of(18, 0);

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_reviewer_id")
    private User managerReviewer;

    @Column(length = 255)
    private String managerComments;

    private LocalDateTime managerReviewedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admin_reviewer_id")
    private User adminReviewer;

    @Column(length = 255)
    private String adminComments;

    private LocalDateTime adminReviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (startDate != null && endDate != null) {
            this.totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
    }

    public AttendanceRegularization() {}

    public AttendanceRegularization(User user, LocalDate startDate, LocalDate endDate, String reason) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        this.reason = reason;
        this.status = RegularizationStatus.PENDING_MANAGER;
        this.checkInTime = LocalTime.of(9, 0);
        this.checkOutTime = LocalTime.of(18, 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }

    public long getManagerApprovedDays() {
        return managerApprovedDays;
    }

    public void setManagerApprovedDays(long managerApprovedDays) {
        this.managerApprovedDays = managerApprovedDays;
    }

    public long getAdminApprovedDays() {
        return adminApprovedDays;
    }

    public void setAdminApprovedDays(long adminApprovedDays) {
        this.adminApprovedDays = adminApprovedDays;
    }

    public long getRemainingAdminDays() {
        if (totalDays <= 7) return 0;
        return totalDays - managerApprovedDays;
    }

    public RegularizationStatus getStatus() {
        return status;
    }

    public void setStatus(RegularizationStatus status) {
        this.status = status;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getManagerReviewer() {
        return managerReviewer;
    }

    public void setManagerReviewer(User managerReviewer) {
        this.managerReviewer = managerReviewer;
    }

    public String getManagerComments() {
        return managerComments;
    }

    public void setManagerComments(String managerComments) {
        this.managerComments = managerComments;
    }

    public LocalDateTime getManagerReviewedAt() {
        return managerReviewedAt;
    }

    public void setManagerReviewedAt(LocalDateTime managerReviewedAt) {
        this.managerReviewedAt = managerReviewedAt;
    }

    public User getAdminReviewer() {
        return adminReviewer;
    }

    public void setAdminReviewer(User adminReviewer) {
        this.adminReviewer = adminReviewer;
    }

    public String getAdminComments() {
        return adminComments;
    }

    public void setAdminComments(String adminComments) {
        this.adminComments = adminComments;
    }

    public LocalDateTime getAdminReviewedAt() {
        return adminReviewedAt;
    }

    public void setAdminReviewedAt(LocalDateTime adminReviewedAt) {
        this.adminReviewedAt = adminReviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
