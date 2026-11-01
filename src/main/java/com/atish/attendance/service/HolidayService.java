package com.atish.attendance.service;

import com.atish.attendance.entity.Holiday;
import com.atish.attendance.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAllByOrderByHolidayDateAsc();
    }

    public List<Holiday> getUpcomingHolidays(LocalDate fromDate) {
        LocalDate start = fromDate != null ? fromDate : LocalDate.now();
        return holidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(start);
    }

    public Optional<Holiday> getNextHoliday(LocalDate fromDate) {
        List<Holiday> upcoming = getUpcomingHolidays(fromDate);
        return upcoming.isEmpty() ? Optional.empty() : Optional.of(upcoming.get(0));
    }

    public boolean isHoliday(LocalDate date) {
        return holidayRepository.findByHolidayDate(date).isPresent();
    }

    public Optional<Holiday> getHolidayOn(LocalDate date) {
        return holidayRepository.findByHolidayDate(date);
    }

    @Transactional
    public Holiday createHoliday(Holiday holiday) {
        if (holiday.getName() == null || holiday.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Holiday name is required.");
        }
        if (holiday.getHolidayDate() == null) {
            throw new IllegalArgumentException("Holiday date is required.");
        }
        if (holidayRepository.findByHolidayDate(holiday.getHolidayDate()).isPresent()) {
            throw new IllegalArgumentException("A holiday is already registered on " + holiday.getHolidayDate());
        }
        holiday.setName(holiday.getName().trim());
        log.info("Registering company holiday: {} on {}", holiday.getName(), holiday.getHolidayDate());
        return holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
        log.info("Deleted holiday with ID {}", id);
    }
}
