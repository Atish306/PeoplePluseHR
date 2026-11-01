package com.atish.attendance.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "leave_type", "calendar_year"})
})
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveType leaveType;

    @Column(nullable = false)
    private double totalQuota = 0.0;

    @Column(nullable = false)
    private double used = 0.0;

    @Column(nullable = false)
    private double pending = 0.0;

    @Column(name = "calendar_year", nullable = false)
    private int calendarYear = 2026;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public LeaveBalance() {}

    public LeaveBalance(User user, LeaveType leaveType, double totalQuota, double used, double pending, int calendarYear) {
        this.user = user;
        this.leaveType = leaveType;
        this.totalQuota = totalQuota;
        this.used = used;
        this.pending = pending;
        this.calendarYear = calendarYear;
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

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public double getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(double totalQuota) {
        this.totalQuota = totalQuota;
    }

    public double getUsed() {
        return used;
    }

    public void setUsed(double used) {
        this.used = used;
    }

    public double getPending() {
        return pending;
    }

    public void setPending(double pending) {
        this.pending = pending;
    }

    public int getCalendarYear() {
        return calendarYear;
    }

    public void setCalendarYear(int calendarYear) {
        this.calendarYear = calendarYear;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getAvailable() {
        if (leaveType == LeaveType.LOSS_OF_PAY || leaveType == LeaveType.UNPAID) {
            return 0.0;
        }
        return Math.max(0.0, totalQuota - used - pending);
    }

    public int getUsagePercent() {
        if (totalQuota <= 0) return 0;
        return (int) Math.min(100, Math.round(((used + pending) / totalQuota) * 100));
    }
}
