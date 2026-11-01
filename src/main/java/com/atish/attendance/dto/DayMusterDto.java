package com.atish.attendance.dto;

import com.atish.attendance.entity.Attendance;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DayMusterDto {
    private LocalDate date;
    private int dayNumber;
    private String dayOfWeekShort;
    private String code; // "R", "H", "P", "A", "L", "-"
    private String label;
    private String badgeClass;
    private String color;
    private String holidayName;
    private Attendance attendance;
    private boolean isToday;
    private boolean isFuture;

    public DayMusterDto() {}

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getDayOfWeekShort() {
        return dayOfWeekShort;
    }

    public void setDayOfWeekShort(String dayOfWeekShort) {
        this.dayOfWeekShort = dayOfWeekShort;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public void setBadgeClass(String badgeClass) {
        this.badgeClass = badgeClass;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToday(boolean today) {
        isToday = today;
    }

    public boolean isFuture() {
        return isFuture;
    }

    public void setFuture(boolean future) {
        isFuture = future;
    }

    public String getFormattedDate() {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd MMM"));
    }
}
