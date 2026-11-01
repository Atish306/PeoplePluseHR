package com.atish.attendance.repository;

import com.atish.attendance.entity.AttendanceRegularization;
import com.atish.attendance.entity.RegularizationStatus;
import com.atish.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegularizationRepository extends JpaRepository<AttendanceRegularization, Long> {
    List<AttendanceRegularization> findByUserOrderByCreatedAtDesc(User user);
    List<AttendanceRegularization> findByStatusOrderByCreatedAtDesc(RegularizationStatus status);
    List<AttendanceRegularization> findAllByOrderByCreatedAtDesc();
    long countByStatus(RegularizationStatus status);
}
