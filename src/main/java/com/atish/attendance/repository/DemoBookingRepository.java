package com.atish.attendance.repository;

import com.atish.attendance.entity.DemoBooking;
import com.atish.attendance.entity.DemoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemoBookingRepository extends JpaRepository<DemoBooking, Long> {

    Optional<DemoBooking> findByBookingReference(String bookingReference);

    List<DemoBooking> findAllByOrderByCreatedAtDesc();

    List<DemoBooking> findByStatusOrderByCreatedAtDesc(DemoStatus status);

    long countByStatus(DemoStatus status);
}
