package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.Registration;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.mapper.RegistrationMapper;
import com.campusflow.service.RegistrationService;
import com.campusflow.service.WaitlistPromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WaitlistPromotionTest extends IntegrationTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private WaitlistPromotionService waitlistPromotionService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Test
    void shouldPromoteFirstWaitlistedStudentAndKeepSeatReserved() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student1 = createUser("student1", UserRole.STUDENT);
        User student2 = createUser("student2", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 1);

        registrationService.register(event.getId(), student1.getId());
        registrationService.register(event.getId(), student2.getId());
        Registration first = registrationMapper.findByEventAndUser(event.getId(), student1.getId());
        Registration second = registrationMapper.findByEventAndUser(event.getId(), student2.getId());

        registrationService.cancel(first.getId(), student1.getId());

        Registration promoted = registrationMapper.findById(second.getId());
        assertThat(promoted.getStatus()).isEqualTo(RegistrationStatus.PROMOTED);
        assertThat(promoted.getConfirmDeadline()).isAfter(LocalDateTime.now());
        assertThat(eventMapper.findById(event.getId()).getOccupiedCount()).isEqualTo(1);

        registrationService.confirm(second.getId(), student2.getId());
        assertThat(registrationMapper.findById(second.getId()).getStatus())
                .isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    void shouldExpirePromotionAndPromoteNextWaitlistedStudent() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student1 = createUser("student1", UserRole.STUDENT);
        User student2 = createUser("student2", UserRole.STUDENT);
        User student3 = createUser("student3", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 1);

        registrationService.register(event.getId(), student1.getId());
        registrationService.register(event.getId(), student2.getId());
        registrationService.register(event.getId(), student3.getId());
        Registration first = registrationMapper.findByEventAndUser(event.getId(), student1.getId());
        Registration second = registrationMapper.findByEventAndUser(event.getId(), student2.getId());
        Registration third = registrationMapper.findByEventAndUser(event.getId(), student3.getId());

        registrationService.cancel(first.getId(), student1.getId());
        jdbcTemplate.update(
                "UPDATE registrations SET confirm_deadline = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)),
                second.getId()
        );
        waitlistPromotionService.expireOne(second.getId());

        assertThat(registrationMapper.findById(second.getId()).getStatus())
                .isEqualTo(RegistrationStatus.EXPIRED);
        assertThat(registrationMapper.findById(third.getId()).getStatus())
                .isEqualTo(RegistrationStatus.PROMOTED);
        assertThat(eventMapper.findById(event.getId()).getOccupiedCount()).isEqualTo(1);
    }
}
