package com.atish.attendance.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "holidays")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 50)
    private String holidayType = "National Holiday";

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean mandatory = true;

    public Holiday() {}

    public Holiday(String name, LocalDate holidayDate, String holidayType, String description, boolean mandatory) {
        this.name = name;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
        this.description = description;
        this.mandatory = mandatory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getFormattedDate() {
        if (holidayDate == null) return "";
        return holidayDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    public String getDayOfWeek() {
        if (holidayDate == null) return "";
        String dow = holidayDate.getDayOfWeek().name();
        return dow.charAt(0) + dow.substring(1).toLowerCase();
    }

    public boolean isPast() {
        if (holidayDate == null) return false;
        return holidayDate.isBefore(LocalDate.now());
    }

    public long getDaysRemaining() {
        if (holidayDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), holidayDate);
    }
}
