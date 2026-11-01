---
name: greythr-attendance-system
description: >-
  Comprehensive architectural reference, runbook, completed deliverables tracker,
  pending backlog, and improvement guidelines for the greytHR Attendance Management System
  (Spring Boot 2.7, Thymeleaf, H2 Database, and Zero-Horizontal-Scroll Responsive UI).
  Use this skill to inspect project status, maintain or extend features, troubleshoot
  operations, and implement upcoming phases.
---

# greytHR Attendance Management System Skill Guide

This skill encapsulates the full technical specification, operational runbook, progress status, pending backlog, and recommended architectural improvements for the **greytHR Attendance Management System**.

---

## 1. System Architecture & Tech Stack

| Layer | Technology / Specification | Key Details |
| :--- | :--- | :--- |
| **Backend Framework** | Spring Boot `2.7.18` (Java 8 compatibility) | Spring MVC, Spring Security, Spring Data JPA |
| **Java Version** | JDK 1.8 (`C:\Java\jdk1.8\bin\java.exe`) | Configured via `run.bat` |
| **Persistence** | Embedded H2 File Database | Location: `./data/attendancedb` (zero-config, persistent) |
| **Frontend Engine** | Thymeleaf 3 + Bootstrap 5.3.3 + Bootstrap Icons | Pure server-side rendering (No React dependency) |
| **Design System** | greytHR Design System (Unpatented/Free Resources) | Navy `#111c38`, Royal Blue `#1b4fc2`, Orange `#ff6a00`, Inter Font |
| **Application Port** | `8085` | Free from Oracle DB / IIS port collisions |
| **Mobile UI Engine** | Offcanvas Drawer + Zero-Horizontal-Scroll Table-to-Card Engine | Pure CSS responsive transform via `data-label` |

---

## 2. What Is Done (Completed Implementations)

### ✅ Phase 1: Security, Roles & Authentication
- **Dual-Pane greytHR Login Screen** (`/login`) with branded enterprise illustration and 1-click test credentials autofill.
- **Spring Security Configuration** (`SecurityConfig.java`): Role-Based Access Control (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_EMPLOYEE`).
- **Custom Success Handler** (`CustomAuthenticationSuccessHandler.java`): Intelligent routing based on role:
  - Admin $\to$ `/admin/dashboard`
  - Manager $\to$ `/manager/dashboard`
  - Employee $\to$ `/dashboard`

### ✅ Phase 2: Employee Self-Service (ESS) & Web Sign-In
- **Live Digital Clock Widget** on Employee Dashboard with real-time seconds ticking.
- **GPS Coordinates Capture** via HTML5 Geolocation (`attendance-geo.js`): captures latitude and longitude at the moment of punch-in.
- **Automatic Status Calculation** (`AttendanceService.java`):
  - In before Shift Start + Grace Period (e.g. 09:15) $\to$ `PRESENT`
  - In after Grace Period $\to$ `LATE`
  - Total duration $< 4.0$ hours at punch-out $\to$ `HALF_DAY`
  - Unclocked employees by midnight $\to$ marked `ABSENT`
- **Location Notes & IP Logging**: Stores optional work remarks and origin IP (`request.getRemoteAddr()`).

### ✅ Phase 3: Shift Management & Scheduling
- **Shift Roster Control** (`/admin/shifts`): General Shift (09:00 - 18:00), Evening Shift (14:00 - 22:00), etc.
- **Configurable Grace Period**: Custom threshold in minutes before flagging attendance as `LATE`.
- **Staff Allocation**: Dynamic drop-down assignment linking employees to custom shifts.

### ✅ Phase 4: Leave Management & Approval Engine
- **Employee Leave Application** (`/leaves/apply`): Select leave type (`CASUAL`, `SICK`, `ANNUAL`), date range, and reason.
- **Manager Approval Portal** (`/manager/leaves`): One-click `Approve` or `Reject` workflow with reviewer audit logging.
- **Attendance Synchronization**: Approved leaves automatically prevent absent penalties on respective dates.
- **Role-Aware Smart Routing** (`/leaves`): Universal endpoint that dynamically routes employees to `/leaves/apply` and managers/admins to `/manager/leaves`.
- **Diagnostic Fix**: Resolved an extraneous dangling Thymeleaf markup block in `leaves/manage.html` (`lr.status.name() == 'PENDING'`) that caused an `EL1007E` 500 error when parsed outside the iteration loop.

### ✅ Phase 5: Automated Midnight Audit & Reconciliation
- **Scheduled Background Job** (`ScheduledAttendanceTasks.java`): Daily cron at `23:59:00` automatically audits unclosed punches and reconciles absentees.
- **Manual Audit Trigger**: Instant admin trigger button (`/admin/audit/run-daily-check`) for on-demand reconciliation.

### ✅ Phase 6: Timesheet Analytics & Interactive Viewer
- **Filter Ledger** (`/reports/summary`): Filter by Start Date, End Date, Employee, Department, and Attendance Status.
- **CSV Data Export** (`/reports/export-csv`): Download payroll-ready CSV timesheet.
- **Interactive Modal Viewer** (`#timesheetModal`): Single-click in-browser inspection showing employee shift, effective hours, overtime/deficit progress bar, GPS map link, and IP verification.
- **Formal Printable Slip** (`/reports/timesheet-slip/{id}`): Formal A4 corporate slip with verification evidence, target variance, and signature lines for Employee, Supervisor, and HR.

### ✅ Phase 7: Mobile Responsiveness & Zero Horizontal Scroll
- **Mobile Offcanvas Drawer**: Left-hand navigation tucks into a smooth drawer with blur backdrop overlay on screens $< 992$px.
- **Hamburger Bar Toggler**: Dedicated `bi-list` icon on topbars and `bi-x-lg` dismiss button inside the drawer header.
- **Zero-Horizontal-Scroll Table Engine** (`.table-mobile-card`):
  - On phones ($< 768$px), multi-column tabular logs transform into vertical card widgets.
  - Every column turns into a key-value row (`[LABEL]` $\to$ `[VALUE]`).
  - Completely eliminates sideways scrolling across all 11 application templates.

### ✅ Phase 8: Full Dark Mode Engine & Mobile Cloud Access
- **Zero-Flicker Theme Engine** (`theme.js`):
  - Self-executing IIFE initializes `data-bs-theme="dark"` or `"light"` before DOM paint to prevent white flash.
  - Respects user preference via `localStorage.getItem('greythr-theme')` with fallback to OS `prefers-color-scheme: dark`.
  - Seamless toggle button with dynamic Sun (`bi-sun-fill`) and Moon (`bi-moon-stars-fill`) icon transitions.
- **greytHR Dark Palette** (`style.css`):
  - Deep slate backgrounds (`#0f172a`), rich container cards (`#1e293b`), slate borders (`#334155`), and contrast text (`#f8fafc`).
  - Translucent pill status badges (`rgba(...)`) preserving greytHR branding in low-light environments.
  - Full dark styling across cards, widgets, inputs, modals, mobile-cards, and tables.
- **Universal Template Coverage**: Integrated into all 11 project templates (`index.html`, `auth/login.html`, all 3 dashboards, leaves, shifts, reports, muster, and printable slips).
- **Mobile Access & Diagnostic Fixes**:
  - Windows Firewall inbound port 8085 opened (`netsh advfirewall firewall add rule name=GreytHR8085`).
  - Embedded mobile QR code widget (`/images/mobile-qr.png`) on home and login pages.
  - Fixed duplicate dangling Thymeleaf markup in `manager-dashboard.html` that caused 500 error on manager login.

### ✅ Phase 9: Viewport-Locked App Shell & Isolated Content Scroll
- **Stationary Sidebar & Topbar**:
  - The outer application wrapper (`.greythr-wrapper`) and main container (`.greythr-main`) are locked to `height: 100vh; height: 100dvh; overflow: hidden;`.
  - Topbar (`.greythr-topbar`) has `flex-shrink: 0`, remaining permanently anchored at the top of the viewport.
  - Sidebar (`.greythr-sidebar`) stays anchored on the left (or offcanvas on mobile), with internal navigation scrolling if items exceed vertical space.
- **Dedicated Content-Only Scroll Pane** (`.greythr-main > main`):
  - Configured with `flex: 1; min-height: 0; overflow-y: auto; overflow-x: hidden; -webkit-overflow-scrolling: touch;`.
  - Prevents the entire browser window or headers from moving during mouse wheel, trackpad, or mobile touch gestures.
- **Sleek Custom Scrollbar**:
  - Subtle 6px rounded thumb scrollbar inside the content pane with adaptive light/dark translucency (`rgba(148, 163, 184, 0.4)`).
- **Sticky Landing Page Navigation**: Added `sticky-top` to `index.html` navbar.

### ✅ Phase 10: Holiday Calendar & Leave Balance Engine
- **Annual Holiday Calendar Portal** (`/holidays`):
  - Pre-seeded 14 official 2026 National and Festival holidays (Republic Day, Holi, Good Friday, Labour Day, Independence Day, Gandhi Jayanti, Dussehra, Diwali, Christmas, etc.).
  - Next Holiday Countdown widget (`nextHoliday`) with dynamic remaining days calculation (`In X days`).
  - Monthly calendar badges (`.holiday-date-badge`) with day of the week, category pills, and mandatory indicators.
  - Role-based Admin management: modal to register new holidays (`/admin/holidays/add`) and delete observances.
- **5-Category Leave Balance Engine** (`LeaveBalance.java` & `LeaveBalanceRepository.java`):
  - **Casual Leave (CL)**: 12 days annual quota
  - **Sick Leave (SL)**: 10 days annual quota
  - **Earned Leave (EL)**: 15 days annual quota
  - **Compensatory Off (Comp-Off)**: 3 days quota for weekend/holiday work
  - **Loss of Pay (LOP)**: Unpaid absence tracker without quota limit
- **Balance Validation & Approval Lifecycle**:
  - Validates requested duration (`ChronoUnit.DAYS.between(start, end) + 1`) against `balance.getAvailable()`.
  - Blocks requests exceeding quota with a friendly warning to adjust dates or select Loss of Pay (LOP).
  - Reserves `pending` days upon employee submission; shifts to `used` on manager approval, or releases on rejection.
- **UI & Dashboard Integrations**:
  - Visual Leave Quota cards on `/leaves/apply` with color-coded progress bars and live duration estimator.
  - Employee workspace quick-balance bar and next-holiday alert on `/dashboard`.
  - Attendance audit integration: Midnight audit recognizes company holidays and flags status as `HOLIDAY` instead of marking absent.

### ✅ Phase 11: Attendance Log Day-Wise Muster & Pie Chart Analytics
- **Day-Wise Month Calendar Muster Grid** (`DayMusterDto.java` & `MonthlyMusterDto.java`):
  - **7-Column Monday Weekday Alignment (`MON` - `SUN`)**:
    - Fixed weekday headers starting on Monday (`MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN`).
    - Dynamic leading empty slots (`startDayOfWeekOffset`) align Day 1 under its exact weekday column (e.g. Thursday for Oct 1).
    - Trailing empty slots (`trailingOffset`) complete the final 7-day row symmetrically.
    - Every Monday lines up under `MON`, through every Sunday under `SUN`.
    - Horizontal scroll container (`.muster-calendar-wrapper` + `.muster-calendar-inner` min-width 760px) guarantees the uniform 7-day format never collapses on mobile screens.
  - Evaluates every calendar day of the requested month with clean badge classification:
    - **`H` (Holiday)**: Company holidays from `HolidayRepository` (evaluated first to guarantee holidays falling on weekends like Diwali are badged `H`). Kept completely clean and empty inside the day card per business rules.
    - **`R` (Rest Day / Weekly Off)**: Saturday and Sunday dates automatically flagged with purple `R` badges and distinctive off-day column styling.
    - **`P` (Present)**: Monday to Friday with recorded clock-in. Displays punch-in/out timestamps and total duration.
    - **`L` (Leave)**: Approved casual, sick, or earned leave on working days.
    - **`A` (Absent)**: Working days (Mon-Fri) without clock-in up to today.
    - **`-` (Upcoming)**: Future days of the month rendered in a neutral state.
- **Visual Pie / Donut Chart Overview** (`Chart.js`):
  - Interactive donut chart visualizing attendance distribution: **Present** (Emerald), **Absent** (Crimson), **Holiday** (Amber), **Rest Day** (Indigo), and **Leave** (Sky Blue).
  - Monthly Attendance & Punctuality rate percentage score card.
  - Quick count KPI summary cards for instant inspection.
- **Interactive Month, Year & Employee Switcher** (`AttendanceController.java`):
  - Accessible at `/attendance/history`.
  - Dropdown controls to inspect any month and year.
  - Admin and Manager role-aware employee switcher to view any team member's day-wise calendar and attendance pie chart.

---

## 3. What Is Pending (Future Deliverables & Roadmap)

| Priority | Feature / Module | Scope & Architecture Needed |
| :---: | :--- | :--- |
| **High** | **Geofencing Radius Enforcement** | Validate that captured GPS latitude/longitude is within $X$ meters of assigned office location (using Haversine distance formula); reject punch if outside radius. |
| **High** | **IP Address Whitelisting** | Restrict employee clock-in to corporate Wi-Fi IP ranges or approved VPN subnets; flag remote punches for manager review. |
| **Medium** | **Email Notifications** | Integrate `spring-boot-starter-mail` to notify employees on leave approval/rejection and alert managers when employees arrive late. |
| **Medium** | **Pagination for Muster Ledger** | Implement Spring Data `Pageable` & `PageRequest` on `AttendanceRepository` to handle tens of thousands of punch records without memory lag. |
| **Medium** | **Biometric Device Integration** | Develop an ingestion REST API (`/api/biometric/sync`) or TCP listener to sync punch logs from ZKTeco/Hikvision hardware clocks. |
| **Low** | **Production MySQL/PostgreSQL Profile** | Add `application-prod.properties` and Docker Compose setup for external relational databases. |
| **Low** | **Automated Test Suite** | Add JUnit 5 / MockMvc tests for `AttendanceService`, `LeaveService`, and Security authorization endpoints. |

---

## 4. Improvements for This Implementation

### 1. Geofence Distance Calculation Helper
Add office latitude, longitude, and allowable radius (meters) to `Department` entity and validate inside `AttendanceService`:
```java
public boolean isWithinOfficeRadius(double empLat, double empLng, double officeLat, double officeLng, double radiusMeters) {
    double earthRadius = 6371000; // meters
    double dLat = Math.toRadians(officeLat - empLat);
    double dLng = Math.toRadians(officeLng - empLng);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
               Math.cos(Math.toRadians(empLat)) * Math.cos(Math.toRadians(officeLat)) *
               Math.sin(dLng / 2) * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = earthRadius * c;
    return distance <= radiusMeters;
}
```

### 2. Spring Data Pagination in Reports
Replace `findAll()` with paginated queries in `ReportController`:
```java
@GetMapping("/summary")
public String viewTimesheetSummary(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    Model model) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("workDate").descending());
    Page<Attendance> records = attendanceRepository.findAll(pageable);
    model.addAttribute("records", records.getContent());
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", records.getTotalPages());
    return "reports/summary";
}
```

### 3. Client IP Resolver Behind Reverse Proxies
Use `X-Forwarded-For` header for accurate client IP resolution behind load balancers:
```java
public String getClientIpAddress(HttpServletRequest request) {
    String xForwarded = request.getHeader("X-Forwarded-For");
    if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
        return xForwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```

---

## 5. Operations & Runbook

### Starting the Server
```cmd
cd /d D:\Atish2\GreytHR
run.bat
```
Or directly using portable Maven:
```cmd
set JAVA_HOME=C:\Java\jdk1.8
set PATH=%JAVA_HOME%\bin;%PATH%
tools\maven\bin\mvn.cmd spring-boot:run
```

### Access URLs & Default Credentials
* **Base URL:** `http://localhost:8085/`
* **H2 Database Console:** `http://localhost:8085/h2-console` (JDBC URL: `jdbc:h2:file:./data/attendancedb`, User: `sa`, Password: empty)

| Role | Username | Password | Default Landing Page | Key Privileges |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | `/admin/dashboard` | Headcount stats, Shifts, Midnight audit, All reports |
| **Manager** | `manager` | `manager123` | `/manager/dashboard` | Team approvals, Department muster, Attendance records |
| **Employee** | `employee` | `employee123` | `/dashboard` | GPS punch in/out, Apply leaves, Personal muster |
