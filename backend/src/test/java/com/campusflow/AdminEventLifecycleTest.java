package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.dto.AdminEventDetailResponse;
import com.campusflow.dto.AdminEventSummaryResponse;
import com.campusflow.dto.AdminRegistrationResponse;
import com.campusflow.dto.EventUpsertRequest;
import com.campusflow.dto.RegistrationResponse;
import com.campusflow.service.EventService;
import com.campusflow.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEventLifecycleTest extends IntegrationTestBase {

    @Autowired
    private EventService eventService;

    @Autowired
    private RegistrationService registrationService;

    @Test
    void shouldPublishFinishAndCloseRegistrationActions() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student = createUser("student1", UserRole.STUDENT);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        EventUpsertRequest request = new EventUpsertRequest(
                "生命周期测试活动",
                "验证草稿、发布、结束和操作权限",
                "测试场地",
                "测试",
                "QA",
                now.plusDays(2).withHour(19),
                now.plusDays(2).withHour(21),
                now.minusHours(1),
                now.plusDays(1).withHour(23),
                1
        );

        AdminEventSummaryResponse created = eventService.create(request, admin.getId());
        assertThat(created.status()).isEqualTo(EventStatus.DRAFT);

        AdminEventDetailResponse detail = eventService.getAdminEventDetail(created.id());
        assertThat(detail.description()).isEqualTo(request.description());
        assertThat(detail.registrationStart()).isEqualTo(request.registrationStart());
        assertThat(detail.remainingSeats()).isEqualTo(1);

        eventService.publish(created.id());
        Event published = eventMapper.findById(created.id());
        assertThat(published.getStatus()).isEqualTo(EventStatus.PUBLISHED);

        RegistrationResponse registration = registrationService.register(created.id(), student.getId());
        assertThat(registration.status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(registration.eventStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(registration.canCancel()).isTrue();
        assertThat(registration.canCheckIn()).isTrue();

        eventService.finish(created.id());

        RegistrationResponse finished = registrationService.findViewForUser(created.id(), student.getId());
        assertThat(finished.eventStatus()).isEqualTo(EventStatus.FINISHED);
        assertThat(finished.canCancel()).isFalse();
        assertThat(finished.canConfirm()).isFalse();
        assertThat(finished.canCheckIn()).isFalse();

        AdminRegistrationResponse adminRow = registrationService.adminRegistrations(created.id()).getFirst();
        assertThat(adminRow.canCheckIn()).isFalse();
    }

    @Test
    void shouldCancelActiveRegistrationsWhenEventIsCanceled() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student = createUser("student1", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 5);
        registrationService.register(event.getId(), student.getId());

        eventService.cancel(event.getId());

        assertThat(eventMapper.findById(event.getId()).getStatus()).isEqualTo(EventStatus.CANCELED);
        assertThat(eventMapper.findById(event.getId()).getOccupiedCount()).isZero();
        assertThat(registrationService.findViewForUser(event.getId(), student.getId()).status())
                .isEqualTo(RegistrationStatus.CANCELED);
        assertThat(registrationService.findViewForUser(event.getId(), student.getId()).canCancel())
                .isFalse();
    }
}
