package com.campusflow.service;

import com.campusflow.config.WaitlistProperties;
import com.campusflow.domain.Event;
import com.campusflow.domain.Registration;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.RegistrationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class WaitlistPromotionService {

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;
    private final WaitlistProperties waitlistProperties;
    private final Clock clock;

    public WaitlistPromotionService(
            EventMapper eventMapper,
            RegistrationMapper registrationMapper,
            WaitlistProperties waitlistProperties,
            Clock clock
    ) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
        this.waitlistProperties = waitlistProperties;
        this.clock = clock;
    }

    @Transactional
    public void promoteNext(Event event) {
        Registration next = registrationMapper.findFirstWaitlistedForUpdate(event.getId());
        if (next == null) {
            eventMapper.decrementOccupied(event.getId());
            return;
        }

        LocalDateTime deadline = LocalDateTime.now(clock)
                .plusMinutes(waitlistProperties.confirmationMinutes());
        registrationMapper.markPromoted(next.getId(), deadline);
    }

    @Transactional
    public void expireOne(Long registrationId) {
        Registration snapshot = registrationMapper.findById(registrationId);
        if (snapshot == null) {
            return;
        }

        Event event = eventMapper.findByIdForUpdate(snapshot.getEventId());
        if (event == null) {
            return;
        }

        Registration registration = registrationMapper.findByIdForUpdate(registrationId);
        if (registration == null || registration.getStatus() != RegistrationStatus.PROMOTED) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (registration.getConfirmDeadline() == null || registration.getConfirmDeadline().isAfter(now)) {
            return;
        }

        registrationMapper.markExpired(registrationId);
        promoteNext(event);
    }
}
