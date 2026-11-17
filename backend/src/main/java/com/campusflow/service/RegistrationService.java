package com.campusflow.service;

import com.campusflow.common.ApiException;
import com.campusflow.domain.AdminRegistrationView;
import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.domain.Registration;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.RegistrationView;
import com.campusflow.dto.AdminRegistrationResponse;
import com.campusflow.dto.CheckInResponse;
import com.campusflow.dto.RegistrationResponse;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.RegistrationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class RegistrationService {

    private static final Set<RegistrationStatus> ACTIVE_STATUSES = EnumSet.of(
            RegistrationStatus.CONFIRMED,
            RegistrationStatus.WAITLISTED,
            RegistrationStatus.PROMOTED,
            RegistrationStatus.CHECKED_IN
    );

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;
    private final WaitlistPromotionService waitlistPromotionService;
    private final CheckInTokenService checkInTokenService;
    private final Clock clock;

    public RegistrationService(
            EventMapper eventMapper,
            RegistrationMapper registrationMapper,
            WaitlistPromotionService waitlistPromotionService,
            CheckInTokenService checkInTokenService,
            Clock clock
    ) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
        this.waitlistPromotionService = waitlistPromotionService;
        this.checkInTokenService = checkInTokenService;
        this.clock = clock;
    }

    @Transactional
    public RegistrationResponse register(Long eventId, Long userId) {
        Event event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!event.isRegistrationOpen(now)) {
            throw ApiException.badRequest("REGISTRATION_CLOSED", "活动当前不可报名");
        }

        Registration existing = registrationMapper.findByEventAndUserForUpdate(eventId, userId);
        if (existing != null && ACTIVE_STATUSES.contains(existing.getStatus())) {
            return toResponse(requireView(eventId, userId));
        }

        boolean hasSeat = event.getOccupiedCount() < event.getCapacity();
        if (existing == null) {
            Registration registration = new Registration();
            registration.setEventId(eventId);
            registration.setUserId(userId);
            registration.setStatus(hasSeat ? RegistrationStatus.CONFIRMED : RegistrationStatus.WAITLISTED);
            registration.setWaitlistTime(hasSeat ? null : now);
            registrationMapper.insert(registration);
        } else if (hasSeat) {
            registrationMapper.markConfirmedOnRegister(existing.getId());
        } else {
            registrationMapper.markWaitlistedOnRegister(existing.getId(), now);
        }

        if (hasSeat) {
            int updated = eventMapper.incrementOccupied(eventId);
            if (updated != 1) {
                throw ApiException.conflict("SEAT_CONFLICT", "名额状态发生变化，请重试");
            }
        }

        return toResponse(requireView(eventId, userId));
    }

    @Transactional
    public RegistrationResponse cancel(Long registrationId, Long userId) {
        Registration snapshot = registrationMapper.findById(registrationId);
        if (snapshot == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "报名记录不存在");
        }

        Event event = eventMapper.findByIdForUpdate(snapshot.getEventId());
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }

        Registration registration = registrationMapper.findByIdForUpdate(registrationId);
        assertOwner(registration, userId);

        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "已签到的报名不能取消");
        }
        if (registration.getStatus() == RegistrationStatus.CANCELED
                || registration.getStatus() == RegistrationStatus.EXPIRED) {
            return toResponse(requireView(registration.getEventId(), userId));
        }

        boolean occupied = registration.getStatus() == RegistrationStatus.CONFIRMED
                || registration.getStatus() == RegistrationStatus.PROMOTED;
        registrationMapper.markCanceled(registrationId, LocalDateTime.now(clock));

        if (occupied) {
            waitlistPromotionService.promoteNext(event);
        }

        return toResponse(requireView(registration.getEventId(), userId));
    }

    @Transactional
    public RegistrationResponse confirm(Long registrationId, Long userId) {
        Registration snapshot = registrationMapper.findById(registrationId);
        if (snapshot == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "报名记录不存在");
        }

        Event event = eventMapper.findByIdForUpdate(snapshot.getEventId());
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }

        Registration registration = registrationMapper.findByIdForUpdate(registrationId);
        assertOwner(registration, userId);
        if (registration.getStatus() != RegistrationStatus.PROMOTED) {
            throw ApiException.conflict("NOT_PROMOTED", "当前报名不在待确认状态");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (registration.getConfirmDeadline() == null || !now.isBefore(registration.getConfirmDeadline())) {
            registrationMapper.markExpired(registrationId);
            waitlistPromotionService.promoteNext(event);
            throw ApiException.conflict("PROMOTION_EXPIRED", "递补确认已过期，名额已释放");
        }

        registrationMapper.markConfirmedAfterPromotion(registrationId);
        return toResponse(requireView(registration.getEventId(), userId));
    }

    public List<RegistrationResponse> myRegistrations(Long userId) {
        return registrationMapper.findViewsByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AdminRegistrationResponse> adminRegistrations(Long eventId) {
        if (eventMapper.findById(eventId) == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        return registrationMapper.findAdminViewsByEventId(eventId).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminRegistrationResponse adminCheckIn(Long registrationId) {
        Registration snapshot = registrationMapper.findById(registrationId);
        if (snapshot == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "报名记录不存在");
        }

        Event event = eventMapper.findById(snapshot.getEventId());
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() == EventStatus.CANCELED) {
            throw ApiException.conflict("EVENT_CANCELED", "活动已取消，不能签到");
        }
        if (event.getStatus() == EventStatus.FINISHED) {
            throw ApiException.conflict("EVENT_FINISHED", "活动已结束，不能签到");
        }

        Registration registration = registrationMapper.findByIdForUpdate(registrationId);
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            return toAdminResponse(registrationMapper.findAdminViewById(registrationId));
        }
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw ApiException.conflict("NOT_CONFIRMED", "只有确认报名的学生可以签到");
        }

        registrationMapper.markCheckedIn(registrationId, LocalDateTime.now(clock));
        return toAdminResponse(registrationMapper.findAdminViewById(registrationId));
    }

    @Transactional
    public CheckInResponse checkInByToken(String token, Long userId) {
        Long eventId = checkInTokenService.parseEventId(token);
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw ApiException.conflict("EVENT_NOT_OPEN", "活动当前不能签到");
        }

        Registration registration = registrationMapper.findByEventAndUserForUpdate(eventId, userId);
        if (registration == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "没有找到该活动的报名记录");
        }
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            return new CheckInResponse(
                    event.getId(),
                    event.getTitle(),
                    registration.getStatus(),
                    registration.getCheckedInAt(),
                    "已完成签到"
            );
        }
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw ApiException.conflict("NOT_CONFIRMED", "只有确认报名的学生可以签到");
        }

        LocalDateTime checkedInAt = LocalDateTime.now(clock);
        registrationMapper.markCheckedIn(registration.getId(), checkedInAt);
        RegistrationView checkedInView = requireView(eventId, userId);
        return new CheckInResponse(
                checkedInView.getEventId(),
                checkedInView.getEventTitle(),
                checkedInView.getStatus(),
                checkedInView.getCheckedInAt(),
                "签到成功"
        );
    }

    public RegistrationResponse findViewForUser(Long eventId, Long userId) {
        RegistrationView view = registrationMapper.findViewByEventAndUser(eventId, userId);
        return view == null ? null : toResponse(view);
    }

    private RegistrationView requireView(Long eventId, Long userId) {
        RegistrationView view = registrationMapper.findViewByEventAndUser(eventId, userId);
        if (view == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "报名记录不存在");
        }
        return view;
    }

    private void assertOwner(Registration registration, Long userId) {
        if (registration == null) {
            throw ApiException.notFound("REGISTRATION_NOT_FOUND", "报名记录不存在");
        }
        if (!registration.getUserId().equals(userId)) {
            throw ApiException.forbidden("NOT_OWNER", "不能操作其他人的报名");
        }
    }

    private RegistrationResponse toResponse(RegistrationView view) {
        Integer waitlistPosition = null;
        if (view.getStatus() == RegistrationStatus.WAITLISTED && view.getWaitlistTime() != null) {
            waitlistPosition = registrationMapper.countWaitlistedBefore(
                    view.getEventId(),
                    view.getWaitlistTime(),
                    view.getId()
            ) + 1;
        }

        return new RegistrationResponse(
                view.getId(),
                view.getEventId(),
                view.getEventTitle(),
                view.getEventStatus(),
                view.getEventStartTime(),
                view.getEventEndTime(),
                view.getEventLocation(),
                view.getStatus(),
                view.getWaitlistTime(),
                waitlistPosition,
                view.getConfirmDeadline(),
                view.getCheckedInAt(),
                view.getCreatedAt(),
                view.getEventStatus() == EventStatus.PUBLISHED
                        && (view.getStatus() == RegistrationStatus.CONFIRMED
                        || view.getStatus() == RegistrationStatus.WAITLISTED
                        || view.getStatus() == RegistrationStatus.PROMOTED),
                view.getEventStatus() == EventStatus.PUBLISHED
                        && view.getStatus() == RegistrationStatus.PROMOTED,
                view.getEventStatus() == EventStatus.PUBLISHED
                        && view.getStatus() == RegistrationStatus.CONFIRMED
        );
    }

    private AdminRegistrationResponse toAdminResponse(AdminRegistrationView view) {
        Integer waitlistPosition = null;
        if (view.getStatus() == RegistrationStatus.WAITLISTED && view.getWaitlistTime() != null) {
            waitlistPosition = registrationMapper.countWaitlistedBefore(
                    view.getEventId(),
                    view.getWaitlistTime(),
                    view.getId()
            ) + 1;
        }

        return new AdminRegistrationResponse(
                view.getId(),
                view.getUserId(),
                view.getUsername(),
                view.getDisplayName(),
                view.getStatus(),
                view.getWaitlistTime(),
                waitlistPosition,
                view.getConfirmDeadline(),
                view.getCheckedInAt(),
                view.getCreatedAt(),
                view.getEventStatus() == EventStatus.PUBLISHED
                        && view.getStatus() == RegistrationStatus.CONFIRMED
        );
    }
}
