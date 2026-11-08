package com.campusflow.service;

import com.campusflow.common.ApiException;
import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.dto.CheckInResponse;
import com.campusflow.dto.CheckInTokenResponse;
import com.campusflow.mapper.EventMapper;
import org.springframework.stereotype.Service;

@Service
public class CheckInService {

    private final EventMapper eventMapper;
    private final CheckInTokenService checkInTokenService;
    private final RegistrationService registrationService;

    public CheckInService(
            EventMapper eventMapper,
            CheckInTokenService checkInTokenService,
            RegistrationService registrationService
    ) {
        this.eventMapper = eventMapper;
        this.checkInTokenService = checkInTokenService;
        this.registrationService = registrationService;
    }

    public CheckInTokenResponse generateToken(Long eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw ApiException.conflict("EVENT_NOT_PUBLISHED", "只有已发布活动可以生成签到码");
        }
        return checkInTokenService.generate(eventId);
    }

    public CheckInResponse checkIn(String token, Long userId) {
        return registrationService.checkInByToken(token, userId);
    }
}
