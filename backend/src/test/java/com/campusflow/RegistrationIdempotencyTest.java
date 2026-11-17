package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.dto.RegistrationResponse;
import com.campusflow.mapper.EventMapper;
import com.campusflow.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationIdempotencyTest extends IntegrationTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private EventMapper eventMapper;

    @Test
    void shouldReturnExistingRegistrationWhenRequestIsRepeated() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student = createUser("student1", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 10);

        RegistrationResponse first = registrationService.register(event.getId(), student.getId());
        RegistrationResponse second = registrationService.register(event.getId(), student.getId());

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(eventMapper.findById(event.getId()).getOccupiedCount()).isEqualTo(1);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND user_id = ?",
                Long.class,
                event.getId(),
                student.getId()
        );
        assertThat(count).isEqualTo(1);
    }
}
