package com.atish.attendance.repository;

import com.atish.attendance.entity.Attendance;
import com.atish.attendance.entity.AttendanceStatus;
import com.atish.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByUserAndWorkDate(User user, LocalDate workDate);
    List<Attendance> findByUserOrderByWorkDateDesc(User user);
    List<Attendance> findByWorkDate(LocalDate workDate);
    long countByWorkDateAndStatus(LocalDate workDate, AttendanceStatus status);
    long countByWorkDate(LocalDate workDate);
    List<Attendance> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);
    List<Attendance> findByUserAndWorkDateBetweenOrderByWorkDateAsc(User user, LocalDate startDate, LocalDate endDate);
}
