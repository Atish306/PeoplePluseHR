package com.atish.attendance.entity;

public enum LeaveType {
    CASUAL("Casual Leave (CL)", "CL", "#3b82f6"),
    SICK("Sick Leave (SL)", "SL", "#10b981"),
    EARNED("Earned Leave (EL)", "EL", "#8b5cf6"),
    COMP_OFF("Compensatory Off (Comp-Off)", "Comp-Off", "#f59e0b"),
    LOSS_OF_PAY("Loss of Pay (LOP)", "LOP", "#ef4444"),
    ANNUAL("Annual Leave (AL)", "AL", "#8b5cf6"),
    UNPAID("Loss of Pay (Unpaid)", "LOP", "#ef4444");

    private final String displayName;
    private final String code;
    private final String color;

    LeaveType(String displayName, String code, String color) {
        this.displayName = displayName;
        this.code = code;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getColor() {
        return color;
    }
}
