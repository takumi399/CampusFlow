package com.campusflow.service;

import com.campusflow.common.ApiException;
import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.dto.AdminEventSummaryResponse;
import com.campusflow.dto.AdminEventDetailResponse;
import com.campusflow.dto.EventDetailResponse;
import com.campusflow.dto.EventSummaryResponse;
import com.campusflow.dto.EventUpsertRequest;
import com.campusflow.dto.RegistrationResponse;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.RegistrationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;
    private final RegistrationService registrationService;
    private final Clock clock;

    public EventService(
            EventMapper eventMapper,
            RegistrationMapper registrationMapper,
            RegistrationService registrationService,
            Clock clock
    ) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
        this.registrationService = registrationService;
        this.clock = clock;
    }

    public List<EventSummaryResponse> listStudentEvents() {
        return eventMapper.findPublished().stream()
                .map(this::toSummary)
                .toList();
    }

    public EventDetailResponse getStudentEvent(Long eventId, Long userId) {
        Event event = eventMapper.findById(eventId);
        if (event == null || event.getStatus() == EventStatus.DRAFT) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        RegistrationResponse registration = registrationService.findViewForUser(eventId, userId);
        return toDetail(event, registration);
    }

    public List<AdminEventSummaryResponse> listAdminEvents() {
        return eventMapper.findAll().stream()
                .map(this::toAdminSummary)
                .toList();
    }

    public AdminEventSummaryResponse getAdminEvent(Long eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        return toAdminSummary(event);
    }

    public AdminEventDetailResponse getAdminEventDetail(Long eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        Counts counts = counts(event.getId());
        return new AdminEventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getCategory(),
                event.getOrganizer(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRegistrationStart(),
                event.getRegistrationEnd(),
                event.getCapacity(),
                event.getOccupiedCount(),
                event.remainingSeats(),
                event.getStatus(),
                counts.confirmed(),
                counts.waitlisted(),
                counts.promoted(),
                counts.checkedIn()
        );
    }

    @Transactional
    public AdminEventSummaryResponse create(EventUpsertRequest request, Long adminId) {
        validate(request);
        Event event = fromRequest(request);
        event.setOccupiedCount(0);
        event.setStatus(EventStatus.DRAFT);
        event.setCreatedBy(adminId);
        eventMapper.insert(event);
        return getAdminEvent(event.getId());
    }

    @Transactional
    public AdminEventSummaryResponse update(Long eventId, EventUpsertRequest request) {
        validate(request);
        Event event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw ApiException.conflict("EVENT_NOT_DRAFT", "只有草稿活动可以编辑");
        }

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setCategory(request.category());
        event.setOrganizer(request.organizer());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setRegistrationStart(request.registrationStart());
        event.setRegistrationEnd(request.registrationEnd());
        event.setCapacity(request.capacity());

        if (eventMapper.updateDraft(event) != 1) {
            throw ApiException.conflict("EVENT_UPDATE_CONFLICT", "活动状态已变化，请刷新后重试");
        }
        return getAdminEvent(eventId);
    }

    @Transactional
    public AdminEventSummaryResponse publish(Long eventId) {
        Event event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw ApiException.conflict("EVENT_NOT_DRAFT", "只有草稿活动可以发布");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!event.getRegistrationEnd().isAfter(now)) {
            throw ApiException.badRequest("INVALID_REGISTRATION_WINDOW", "报名结束时间必须晚于当前时间");
        }
        if (!event.getStartTime().isAfter(now)) {
            throw ApiException.badRequest("INVALID_EVENT_TIME", "活动开始时间必须晚于当前时间");
        }
        if (eventMapper.publish(eventId) != 1) {
            throw ApiException.conflict("EVENT_PUBLISH_CONFLICT", "活动状态已变化，请刷新后重试");
        }
        return getAdminEvent(eventId);
    }

    @Transactional
    public AdminEventSummaryResponse cancel(Long eventId) {
        Event event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw ApiException.conflict("EVENT_NOT_PUBLISHED", "只有已发布活动可以取消");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (eventMapper.cancel(eventId) != 1) {
            throw ApiException.conflict("EVENT_CANCEL_CONFLICT", "活动状态已变化，请刷新后重试");
        }
        registrationMapper.cancelAllActiveByEvent(eventId, now);
        eventMapper.resetOccupied(eventId);
        return getAdminEvent(eventId);
    }

    @Transactional
    public AdminEventSummaryResponse finish(Long eventId) {
        Event event = eventMapper.findByIdForUpdate(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw ApiException.conflict("EVENT_NOT_PUBLISHED", "只有已发布活动可以结束");
        }
        if (eventMapper.finish(eventId) != 1) {
            throw ApiException.conflict("EVENT_FINISH_CONFLICT", "活动状态已变化，请刷新后重试");
        }
        return getAdminEvent(eventId);
    }

    private void validate(EventUpsertRequest request) {
        if (request.capacity() == null || request.capacity() < 1) {
            throw ApiException.badRequest("INVALID_CAPACITY", "活动名额至少为 1");
        }
        if (!request.registrationStart().isBefore(request.registrationEnd())) {
            throw ApiException.badRequest("INVALID_REGISTRATION_WINDOW", "报名开始时间必须早于报名结束时间");
        }
        if (request.registrationEnd().isAfter(request.startTime())) {
            throw ApiException.badRequest("INVALID_REGISTRATION_WINDOW", "报名结束时间不能晚于活动开始时间");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw ApiException.badRequest("INVALID_EVENT_TIME", "活动开始时间必须早于结束时间");
        }
    }

    private Event fromRequest(EventUpsertRequest request) {
        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setCategory(request.category());
        event.setOrganizer(request.organizer());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setRegistrationStart(request.registrationStart());
        event.setRegistrationEnd(request.registrationEnd());
        event.setCapacity(request.capacity());
        return event;
    }

    private EventSummaryResponse toSummary(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getOrganizer(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRegistrationStart(),
                event.getRegistrationEnd(),
                event.getCapacity(),
                event.getOccupiedCount(),
                event.remainingSeats(),
                event.getStatus()
        );
    }

    private EventDetailResponse toDetail(Event event, RegistrationResponse registration) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getOrganizer(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getRegistrationStart(),
                event.getRegistrationEnd(),
                event.getCapacity(),
                event.getOccupiedCount(),
                event.remainingSeats(),
                event.getStatus(),
                registration
        );
    }

    private AdminEventSummaryResponse toAdminSummary(Event event) {
        Counts counts = counts(event.getId());

        return new AdminEventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus(),
                event.getStartTime(),
                event.getRegistrationEnd(),
                event.getCapacity(),
                event.getOccupiedCount(),
                event.remainingSeats(),
                counts.confirmed(),
                counts.waitlisted(),
                counts.promoted(),
                counts.checkedIn()
        );
    }

    private Counts counts(Long eventId) {
        return new Counts(
                registrationMapper.countByEventAndStatus(eventId, RegistrationStatus.CONFIRMED),
                registrationMapper.countByEventAndStatus(eventId, RegistrationStatus.WAITLISTED),
                registrationMapper.countByEventAndStatus(eventId, RegistrationStatus.PROMOTED),
                registrationMapper.countByEventAndStatus(eventId, RegistrationStatus.CHECKED_IN)
        );
    }

    private record Counts(long confirmed, long waitlisted, long promoted, long checkedIn) {
    }
}
