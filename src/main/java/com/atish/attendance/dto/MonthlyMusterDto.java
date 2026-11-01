package com.atish.attendance.dto;

import java.util.ArrayList;
import java.util.List;

public class MonthlyMusterDto {
    private int year;
    private int month;
    private String monthName;
    private int totalDays;
    private int presentCount;
    private int absentCount;
    private int holidayCount;
    private int restDayCount;
    private int leaveCount;
    private int workingDaysCount;
    private double attendancePercentage;
    private int startDayOfWeekOffset; // Number of leading empty slots before day 1 (0 if Monday ... 6 if Sunday)
    private int trailingOffset; // Number of trailing empty slots to complete the final 7-day row
    private List<DayMusterDto> days = new ArrayList<>();

    public MonthlyMusterDto() {}

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public int getHolidayCount() {
        return holidayCount;
    }

    public void setHolidayCount(int holidayCount) {
        this.holidayCount = holidayCount;
    }

    public int getRestDayCount() {
        return restDayCount;
    }

    public void setRestDayCount(int restDayCount) {
        this.restDayCount = restDayCount;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }

    public int getWorkingDaysCount() {
        return workingDaysCount;
    }

    public void setWorkingDaysCount(int workingDaysCount) {
        this.workingDaysCount = workingDaysCount;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getStartDayOfWeekOffset() {
        return startDayOfWeekOffset;
    }

    public void setStartDayOfWeekOffset(int startDayOfWeekOffset) {
        this.startDayOfWeekOffset = startDayOfWeekOffset;
    }

    public int getTrailingOffset() {
        return trailingOffset;
    }

    public void setTrailingOffset(int trailingOffset) {
        this.trailingOffset = trailingOffset;
    }

    public List<DayMusterDto> getDays() {
        return days;
    }

    public void setDays(List<DayMusterDto> days) {
        this.days = days;
    }
}
