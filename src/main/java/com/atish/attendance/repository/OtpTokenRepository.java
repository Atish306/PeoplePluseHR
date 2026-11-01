package com.atish.attendance.repository;

import com.atish.attendance.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc(String identifier);
}
