package com.atish.attendance.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "work_date"})
})
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    private LocalDateTime punchInTime;
    private LocalDateTime punchOutTime;

    private Double inLatitude;
    private Double inLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    private Double totalHours = 0.0;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String notes;

    private Double distanceMeters;
    private Boolean withinGeofence = true;

    public Attendance() {}

    public Attendance(User user, LocalDate workDate, LocalDateTime punchInTime, AttendanceStatus status) {
        this.user = user;
        this.workDate = workDate;
        this.punchInTime = punchInTime;
        this.status = status;
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

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public LocalDateTime getPunchInTime() {
        return punchInTime;
    }

    public void setPunchInTime(LocalDateTime punchInTime) {
        this.punchInTime = punchInTime;
    }

    public LocalDateTime getPunchOutTime() {
        return punchOutTime;
    }

    public void setPunchOutTime(LocalDateTime punchOutTime) {
        this.punchOutTime = punchOutTime;
    }

    public Double getInLatitude() {
        return inLatitude;
    }

    public void setInLatitude(Double inLatitude) {
        this.inLatitude = inLatitude;
    }

    public Double getInLongitude() {
        return inLongitude;
    }

    public void setInLongitude(Double inLongitude) {
        this.inLongitude = inLongitude;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Boolean getWithinGeofence() {
        return withinGeofence;
    }

    public void setWithinGeofence(Boolean withinGeofence) {
        this.withinGeofence = withinGeofence;
    }
}
