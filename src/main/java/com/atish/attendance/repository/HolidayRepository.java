package com.atish.attendance.repository;

import com.atish.attendance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByHolidayDate(LocalDate holidayDate);
    List<Holiday> findAllByOrderByHolidayDateAsc();
    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate startDate, LocalDate endDate);
    List<Holiday> findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate date);
}
