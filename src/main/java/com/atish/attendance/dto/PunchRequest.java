package com.atish.attendance.dto;

public class PunchRequest {

    private Double latitude;
    private Double longitude;
    private String notes;

    public PunchRequest() {}

    public PunchRequest(Double latitude, Double longitude, String notes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
