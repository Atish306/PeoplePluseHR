package com.atish.attendance.util;

import com.atish.attendance.entity.Attendance;

import java.io.PrintWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExportUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public static void writeAttendanceToCsv(Writer writer, List<Attendance> records) {
        PrintWriter pw = new PrintWriter(writer);

        // Header Row
        pw.println("Record ID,Date,Employee Name,Email,Department,Shift,Check In,Check Out,Total Hours,Status,IP Address,Notes");

        for (Attendance rec : records) {
            String id = rec.getId() != null ? rec.getId().toString() : "";
            String date = rec.getWorkDate() != null ? rec.getWorkDate().toString() : "";
            String name = rec.getUser() != null ? escapeCsv(rec.getUser().getFullName()) : "";
            String email = rec.getUser() != null ? escapeCsv(rec.getUser().getEmail()) : "";
            String dept = (rec.getUser() != null && rec.getUser().getDepartment() != null)
                    ? escapeCsv(rec.getUser().getDepartment().getName()) : "General";
            String shift = (rec.getUser() != null && rec.getUser().getShift() != null)
                    ? escapeCsv(rec.getUser().getShift().getName()) : "General Shift";

            String checkIn = rec.getPunchInTime() != null ? rec.getPunchInTime().format(TIME_FMT) : "--";
            String checkOut = rec.getPunchOutTime() != null ? rec.getPunchOutTime().format(TIME_FMT) : "--";
            String hours = rec.getTotalHours() != null ? rec.getTotalHours().toString() : "0.0";
            String status = rec.getStatus() != null ? rec.getStatus().name() : "";
            String ip = rec.getIpAddress() != null ? escapeCsv(rec.getIpAddress()) : "";
            String notes = rec.getNotes() != null ? escapeCsv(rec.getNotes()) : "";

            pw.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    id, date, name, email, dept, shift, checkIn, checkOut, hours, status, ip, notes));
        }

        pw.flush();
    }

    private static String escapeCsv(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }
}
