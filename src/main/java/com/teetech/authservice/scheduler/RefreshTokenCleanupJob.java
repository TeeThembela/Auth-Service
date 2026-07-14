package com.teetech.authservice.scheduler;

import com.teetech.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupJob {
    private final RefreshTokenRepository repository;

    @Scheduled(cron = "${application.scheduler.refresh-token-cleanup-cron}")
    @Transactional
    public void cleanUpExpiredRefreshTokens() {
        log.info("Starting expired refresh token cleanup");

        int deleted = repository.deleteAllExpired(Instant.now());

        if (deleted > 0) {
            log.info("Cleanup complete, deleted {} expired refresh token(s)", deleted);
        } else {
            log.debug("Cleanup complete, no expired tokens found");
        }
    }
}
