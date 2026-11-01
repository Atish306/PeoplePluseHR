package com.atish.attendance.service;

import com.atish.attendance.entity.Shift;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.ShiftRepository;
import com.atish.attendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;

    public ShiftService(ShiftRepository shiftRepository, UserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with ID: " + id));
    }

    @Transactional
    public Shift createShift(String name, LocalTime startTime, LocalTime endTime, int gracePeriodMinutes) {
        Shift shift = new Shift(name, startTime, endTime, gracePeriodMinutes);
        return shiftRepository.save(shift);
    }

    @Transactional
    public void assignShiftToUser(Long userId, Long shiftId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Shift shift = getShiftById(shiftId);
        user.setShift(shift);
        userRepository.save(user);
    }
}
