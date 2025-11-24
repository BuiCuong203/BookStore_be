package com.vn.backend.job;

import com.vn.backend.repository.InvalidTokenRepository;
import com.vn.backend.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class InvalidTokenCleanupJob {
    @Autowired
    private InvalidTokenRepository invalidTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${jobs.cleanup.cron}", zone = "${jobs.cleanup.zone}")
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        long deletedCount1 = invalidTokenRepository.deleteExpired(now);
        long deletedCount2 = refreshTokenRepository.deleteExpired(now);

        if (deletedCount1 > 0) {
            log.info("Cleaned InvalidToken: {}, RefreshToken: {} expired tokens at {}", deletedCount1, deletedCount2, now);
        } else {
            log.debug("No expired tokens to clean at {}", now);
        }
    }
}
