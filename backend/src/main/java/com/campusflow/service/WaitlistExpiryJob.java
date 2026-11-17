package com.campusflow.service;

import com.campusflow.mapper.RegistrationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WaitlistExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(WaitlistExpiryJob.class);

    private final RegistrationMapper registrationMapper;
    private final WaitlistPromotionService waitlistPromotionService;
    private final Clock clock;

    public WaitlistExpiryJob(
            RegistrationMapper registrationMapper,
            WaitlistPromotionService waitlistPromotionService,
            Clock clock
    ) {
        this.registrationMapper = registrationMapper;
        this.waitlistPromotionService = waitlistPromotionService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.waitlist.expiry-scan-ms:30000}")
    public void expireDuePromotions() {
        List<Long> ids = registrationMapper.findExpiredPromotionIds(LocalDateTime.now(clock), 100);
        for (Long id : ids) {
            try {
                waitlistPromotionService.expireOne(id);
            } catch (RuntimeException exception) {
                log.warn("Failed to expire promotion registrationId={}", id, exception);
            }
        }
    }
}
