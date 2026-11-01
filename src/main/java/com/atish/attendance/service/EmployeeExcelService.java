package com.atish.attendance.service;

import com.atish.attendance.entity.Department;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.Shift;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.DepartmentRepository;
import com.atish.attendance.repository.ShiftRepository;
import com.atish.attendance.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeExcelService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeExcelService.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveService leaveService;

    public EmployeeExcelService(UserRepository userRepository,
                                DepartmentRepository departmentRepository,
                                ShiftRepository shiftRepository,
                                PasswordEncoder passwordEncoder,
                                LeaveService leaveService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.shiftRepository = shiftRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveService = leaveService;
    }

    public static class ImportResult {
        private int totalRows = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private List<String> createdUsers = new ArrayList<>();
        private List<String> errorMessages = new ArrayList<>();

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public List<String> getCreatedUsers() { return createdUsers; }
        public List<String> getErrorMessages() { return errorMessages; }
    }

    /**
     * Generates a starter Excel template (.xlsx) for employee bulk upload.
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] columns = {"Username*", "Full Name*", "Email*", "Mobile*", "Role (EMPLOYEE/MANAGER/ADMIN)", "Department (IT/HR/OPS)", "Shift", "Password"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Row 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("rahul.sharma");
            row1.createCell(1).setCellValue("Rahul Sharma");
            row1.createCell(2).setCellValue("rahul.sharma@company.com");
            row1.createCell(3).setCellValue("9876543211");
            row1.createCell(4).setCellValue("EMPLOYEE");
            row1.createCell(5).setCellValue("IT");
            row1.createCell(6).setCellValue("General Shift");
            row1.createCell(7).setCellValue("emp123");

            // Sample Row 2
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("priya.patil");
            row2.createCell(1).setCellValue("Priya Patil");
            row2.createCell(2).setCellValue("priya.patil@company.com");
            row2.createCell(3).setCellValue("9876543212");
            row2.createCell(4).setCellValue("EMPLOYEE");
            row2.createCell(5).setCellValue("HR");
            row2.createCell(6).setCellValue("General Shift");
            row2.createCell(7).setCellValue("emp123");

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Imports employees from an uploaded Excel spreadsheet (.xlsx / .xls).
     */
    @Transactional
    public ImportResult importEmployees(MultipartFile file) throws IOException {
        ImportResult result = new ImportResult();
        if (file == null || file.isEmpty()) {
            result.getErrorMessages().add("The uploaded file is empty. Please select a valid Excel file.");
            return result;
        }

        Department defaultDept = departmentRepository.findAll().stream().findFirst().orElse(null);
        Shift defaultShift = shiftRepository.findAll().stream().findFirst().orElse(null);

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 1) {
                result.getErrorMessages().add("No data rows found in the sheet. Please make sure data starts from row 2.");
                return result;
            }

            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                result.setTotalRows(result.getTotalRows() + 1);
                int rowNum = r + 1;

                String username = getCellValue(row.getCell(0)).trim();
                String fullName = getCellValue(row.getCell(1)).trim();
                String email = getCellValue(row.getCell(2)).trim().toLowerCase();
                String mobile = getCellValue(row.getCell(3)).trim();
                String roleStr = getCellValue(row.getCell(4)).trim().toUpperCase();
                String deptCode = getCellValue(row.getCell(5)).trim();
                String shiftName = getCellValue(row.getCell(6)).trim();
                String rawPassword = getCellValue(row.getCell(7)).trim();

                // Validation
                if (username.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.getErrorMessages().add("Row " + rowNum + ": Missing required field (Username, Full Name, or Email).");
                    continue;
                }

                if (userRepository.existsByUsername(username)) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.getErrorMessages().add("Row " + rowNum + ": Username '" + username + "' is already taken.");
                    continue;
                }

                if (userRepository.existsByEmail(email)) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.getErrorMessages().add("Row " + rowNum + ": Email '" + email + "' is already registered.");
                    continue;
                }

                if (!mobile.isEmpty() && userRepository.existsByMobile(mobile)) {
                    result.setFailureCount(result.getFailureCount() + 1);
                    result.getErrorMessages().add("Row " + rowNum + ": Mobile '" + mobile + "' is already registered.");
                    continue;
                }

                // Determine Role
                Role role = Role.ROLE_EMPLOYEE;
                if ("ADMIN".equals(roleStr) || "ROLE_ADMIN".equals(roleStr)) {
                    role = Role.ROLE_ADMIN;
                } else if ("MANAGER".equals(roleStr) || "ROLE_MANAGER".equals(roleStr)) {
                    role = Role.ROLE_MANAGER;
                }

                // Determine Department
                Department department = defaultDept;
                if (!deptCode.isEmpty()) {
                    Department foundDept = departmentRepository.findByCode(deptCode)
                            .orElseGet(() -> departmentRepository.findByName(deptCode).orElse(null));
                    if (foundDept != null) {
                        department = foundDept;
                    }
                }

                // Determine Shift
                Shift shift = defaultShift;
                if (!shiftName.isEmpty()) {
                    Shift foundShift = shiftRepository.findByName(shiftName).orElse(null);
                    if (foundShift != null) {
                        shift = foundShift;
                    }
                }

                // Password
                if (rawPassword.isEmpty()) {
                    rawPassword = "employee123";
                }
                String encodedPassword = passwordEncoder.encode(rawPassword);

                // Create User
                User newUser = new User(username, email, mobile.isEmpty() ? null : mobile, encodedPassword, fullName, role, department, shift);
                User savedUser = userRepository.save(newUser);

                // Initialize 2026 Leave Balances for employee
                try {
                    leaveService.getUserLeaveBalances(savedUser, 2026);
                } catch (Exception ex) {
                    log.warn("Could not pre-seed leave balance for imported user: {}", username, ex);
                }

                result.setSuccessCount(result.getSuccessCount() + 1);
                result.getCreatedUsers().add(fullName + " (" + username + " - " + email + ")");
            }
        }

        return result;
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                long val = (long) cell.getNumericCellValue();
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
