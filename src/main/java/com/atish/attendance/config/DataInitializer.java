package com.atish.attendance.config;

import com.atish.attendance.entity.*;
import com.atish.attendance.repository.*;
import com.atish.attendance.service.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final HolidayRepository holidayRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final CountryRepository countryRepository;
    private final SystemSettingRepository systemSettingRepository;

    public DataInitializer(DepartmentRepository departmentRepository,
                           ShiftRepository shiftRepository,
                           UserRepository userRepository,
                           AttendanceRepository attendanceRepository,
                           PasswordEncoder passwordEncoder,
                           HolidayRepository holidayRepository,
                           LeaveBalanceRepository leaveBalanceRepository,
                           CountryRepository countryRepository,
                           SystemSettingRepository systemSettingRepository) {
        this.departmentRepository = departmentRepository;
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.holidayRepository = holidayRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.countryRepository = countryRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and seeding default system data...");

        // 1. Seed Departments
        Department itDept = departmentRepository.findByCode("IT").orElseGet(() ->
                departmentRepository.save(new Department("Information Technology", "IT")));

        Department hrDept = departmentRepository.findByCode("HR").orElseGet(() ->
                departmentRepository.save(new Department("Human Resources", "HR")));

        Department opsDept = departmentRepository.findByCode("OPS").orElseGet(() ->
                departmentRepository.save(new Department("Operations", "OPS")));

        // 2. Seed Shifts
        Shift generalShift = shiftRepository.findByName("General Shift").orElseGet(() ->
                shiftRepository.save(new Shift("General Shift", LocalTime.of(9, 0), LocalTime.of(18, 0), 15)));

        Shift morningShift = shiftRepository.findByName("Morning Shift").orElseGet(() ->
                shiftRepository.save(new Shift("Morning Shift", LocalTime.of(7, 0), LocalTime.of(15, 30), 10)));

        // 3. Seed Users
        // Requirement 1: Seed Super Admin
        User superAdminUser = userRepository.findByUsername("superadmin").orElseGet(() -> {
            User sa = new User(
                    "superadmin",
                    "superadmin@company.com",
                    "9999999999",
                    passwordEncoder.encode("superadmin123"),
                    "Executive Super Admin",
                    Role.ROLE_SUPER_ADMIN,
                    itDept,
                    generalShift
            );
            return userRepository.save(sa);
        });

        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User(
                    "admin",
                    "admin@company.com",
                    "9876543201",
                    passwordEncoder.encode("admin123"),
                    "System Administrator",
                    Role.ROLE_ADMIN,
                    itDept,
                    generalShift
            );
            return userRepository.save(admin);
        });

        User managerUser = userRepository.findByUsername("manager").orElseGet(() -> {
            User manager = new User(
                    "manager",
                    "manager@company.com",
                    "9876543202",
                    passwordEncoder.encode("manager123"),
                    "Operations Manager",
                    Role.ROLE_MANAGER,
                    opsDept,
                    generalShift
            );
            return userRepository.save(manager);
        });

        User employeeUser = userRepository.findByUsername("employee").orElseGet(() -> {
            User employee = new User(
                    "employee",
                    "employee@company.com",
                    "9876543210",
                    passwordEncoder.encode("employee123"),
                    "Alex Smith",
                    Role.ROLE_EMPLOYEE,
                    itDept,
                    generalShift
            );
            return userRepository.save(employee);
        });

        // Ensure mobile numbers exist on already-existing seeded records
        if (employeeUser.getMobile() == null || employeeUser.getMobile().isEmpty()) {
            employeeUser.setMobile("9876543210");
            userRepository.save(employeeUser);
        }
        if (adminUser.getMobile() == null || adminUser.getMobile().isEmpty()) {
            adminUser.setMobile("9876543201");
            userRepository.save(adminUser);
        }

        // 4. Seed sample Attendance for Employee (Today)
        LocalDate today = LocalDate.now();
        if (!attendanceRepository.findByUserAndWorkDate(employeeUser, today).isPresent()) {
            Attendance sampleAttendance = new Attendance(
                    employeeUser,
                    today,
                    LocalDateTime.of(today, LocalTime.of(9, 5)),
                    AttendanceStatus.PRESENT
            );
            sampleAttendance.setPunchOutTime(LocalDateTime.of(today, LocalTime.of(18, 2)));
            sampleAttendance.setTotalHours(8.95);
            sampleAttendance.setInLatitude(28.6139);
            sampleAttendance.setInLongitude(77.2090);
            sampleAttendance.setIpAddress("127.0.0.1");
            sampleAttendance.setDistanceMeters(45.0);
            sampleAttendance.setWithinGeofence(true);
            sampleAttendance.setNotes("Punched in on time");
            attendanceRepository.save(sampleAttendance);
            log.info("Sample attendance record seeded for user: employee");
        }

        // 5. Seed 2026 Company Holiday Calendar
        seedHolidays();

        // 6. Seed Leave Balances for 2026 (CL, SL, EL, Comp-Off, LOP)
        seedLeaveBalances(superAdminUser);
        seedLeaveBalances(employeeUser);
        seedLeaveBalances(managerUser);
        seedLeaveBalances(adminUser);

        // 7. Seed Country Master Data
        seedCountries();

        // 8. Seed Default System Setting for Admin Employee Visibility
        systemSettingRepository.findByKey(SystemSettingService.KEY_ADMIN_VIEW_EMP_DETAILS).orElseGet(() ->
                systemSettingRepository.save(new SystemSetting(
                        SystemSettingService.KEY_ADMIN_VIEW_EMP_DETAILS,
                        "true",
                        "Controls whether Admin users are allowed to view employee details"
                )));

        log.info("System data seeding completed successfully.");
        log.info("Default Accounts Created:");
        log.info("  Super Admin: superadmin / superadmin123");
        log.info("  Admin:       admin / admin123");
        log.info("  Manager:     manager / manager123");
        log.info("  Employee:    employee / employee123 (Mobile: 9876543210, Email: employee@company.com)");
    }

    private void seedHolidays() {
        List<Holiday> defaultHolidays = Arrays.asList(
                new Holiday("New Year's Day", LocalDate.of(2026, 1, 1), "National Holiday", "Celebration of the New Calendar Year", true),
                new Holiday("Republic Day", LocalDate.of(2026, 1, 26), "National Holiday", "Honoring the Constitution of India", true),
                new Holiday("Maha Shivratri", LocalDate.of(2026, 3, 4), "Festival", "Traditional Festival of Lord Shiva", false),
                new Holiday("Holi (Festival of Colors)", LocalDate.of(2026, 3, 14), "Festival", "National Celebration of Spring & Colors", true),
                new Holiday("Good Friday", LocalDate.of(2026, 4, 3), "National Holiday", "Christian Holy Day commemorating the Passion", true),
                new Holiday("Eid al-Fitr", LocalDate.of(2026, 4, 20), "Festival", "Islamic Holy Day marking end of Ramadan", true),
                new Holiday("Independence Day", LocalDate.of(2026, 8, 15), "National Holiday", "Celebration of Indian Independence", true),
                new Holiday("Mahatma Gandhi Jayanti", LocalDate.of(2026, 10, 2), "National Holiday", "Birthday of the Father of the Nation", true),
                new Holiday("Dussehra (Vijayadashami)", LocalDate.of(2026, 10, 20), "Festival", "Victory of Good over Evil", true),
                new Holiday("Diwali (Deepavali)", LocalDate.of(2026, 11, 8), "Festival", "Festival of Lights", true),
                new Holiday("Christmas Day", LocalDate.of(2026, 12, 25), "National Holiday", "Celebration of the Nativity of Jesus Christ", true)
        );

        for (Holiday holiday : defaultHolidays) {
            if (!holidayRepository.findByHolidayDate(holiday.getHolidayDate()).isPresent()) {
                holidayRepository.save(holiday);
            }
        }
    }

    private void seedLeaveBalances(User user) {
        int year = 2026;
        seedBalanceIfMissing(user, LeaveType.CASUAL, 12.0, year);
        seedBalanceIfMissing(user, LeaveType.SICK, 10.0, year);
        seedBalanceIfMissing(user, LeaveType.EARNED, 15.0, year);
        seedBalanceIfMissing(user, LeaveType.COMP_OFF, 3.0, year);
        seedBalanceIfMissing(user, LeaveType.LOSS_OF_PAY, 0.0, year);
    }

    private void seedBalanceIfMissing(User user, LeaveType type, double quota, int year) {
        if (!leaveBalanceRepository.findByUserAndLeaveTypeAndCalendarYear(user, type, year).isPresent()) {
            LeaveBalance balance = new LeaveBalance(user, type, quota, 0.0, 0.0, year);
            leaveBalanceRepository.save(balance);
        }
    }

    private void seedCountries() {
        if (countryRepository.count() > 0) {
            return;
        }

        List<Country> countries = Arrays.asList(
                new Country("IN", "India", "+91", 1),
                new Country("AE", "United Arab Emirates", "+971", 2),
                new Country("SA", "Saudi Arabia", "+966", 3),
                new Country("QA", "Qatar", "+974", 4),
                new Country("OM", "Oman", "+968", 5),
                new Country("KW", "Kuwait", "+965", 6),
                new Country("BH", "Bahrain", "+973", 7),
                new Country("US", "United States", "+1", 8),
                new Country("GB", "United Kingdom", "+44", 9),
                new Country("SG", "Singapore", "+65", 10),
                new Country("MY", "Malaysia", "+60", 11),
                new Country("AU", "Australia", "+61", 12),
                new Country("CA", "Canada", "+1", 13),
                new Country("DE", "Germany", "+49", 14)
        );

        countryRepository.saveAll(countries);
    }
}
