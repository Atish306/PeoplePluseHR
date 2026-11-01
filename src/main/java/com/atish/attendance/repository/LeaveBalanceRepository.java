package com.atish.attendance.repository;

import com.atish.attendance.entity.LeaveBalance;
import com.atish.attendance.entity.LeaveType;
import com.atish.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserAndCalendarYearOrderByLeaveTypeAsc(User user, int calendarYear);
    Optional<LeaveBalance> findByUserAndLeaveTypeAndCalendarYear(User user, LeaveType leaveType, int calendarYear);
    List<LeaveBalance> findByUser(User user);
}
