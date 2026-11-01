package com.atish.attendance.repository;

import com.atish.attendance.entity.LeaveRequest;
import com.atish.attendance.entity.LeaveStatus;
import com.atish.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserOrderByCreatedAtDesc(User user);
    List<LeaveRequest> findByUserAndStatus(User user, LeaveStatus status);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l WHERE l.user = :user AND l.status = 'APPROVED' AND :workDate BETWEEN l.startDate AND l.endDate")
    List<LeaveRequest> findApprovedLeaveOnDate(@Param("user") User user, @Param("workDate") LocalDate workDate);
}
