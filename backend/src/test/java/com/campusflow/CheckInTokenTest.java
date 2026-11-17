package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.config.CheckInProperties;
import com.campusflow.common.ApiException;
import com.campusflow.dto.CheckInResponse;
import com.campusflow.mapper.RegistrationMapper;
import com.campusflow.service.CheckInService;
import com.campusflow.service.RegistrationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckInTokenTest extends IntegrationTestBase {

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private CheckInProperties checkInProperties;

    @Test
    void shouldCheckInOnceAndBeIdempotent() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student = createUser("student1", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 10);
        registrationService.register(event.getId(), student.getId());

        String token = checkInService.generateToken(event.getId()).token();
        CheckInResponse firstCheckIn = checkInService.checkIn(token, student.getId());
        CheckInResponse repeatedCheckIn = checkInService.checkIn(token, student.getId());

        assertThat(registrationMapper.findByEventAndUser(event.getId(), student.getId()).getStatus())
                .isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(firstCheckIn.checkedInAt()).isNotNull();
        assertThat(repeatedCheckIn.checkedInAt()).isEqualTo(firstCheckIn.checkedInAt());
        Long checkedInCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND checked_in_at IS NOT NULL",
                Long.class,
                event.getId()
        );
        assertThat(checkedInCount).isEqualTo(1);
    }

    @Test
    void shouldRejectExpiredCheckInToken() {
        User admin = createUser("admin", UserRole.ADMIN);
        User student = createUser("student1", UserRole.STUDENT);
        Event event = createEvent(admin.getId(), 10);
        registrationService.register(event.getId(), student.getId());

        SecretKey key = Keys.hmacShaKeyFor(
                checkInProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
        String expiredToken = Jwts.builder()
                .subject(String.valueOf(event.getId()))
                .claim("type", "CHECK_IN")
                .issuedAt(Date.from(Instant.now().minusSeconds(600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> checkInService.checkIn(expiredToken, student.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessage("签到二维码已过期或无效");
        assertThat(registrationMapper.findByEventAndUser(event.getId(), student.getId()).getStatus())
                .isEqualTo(RegistrationStatus.CONFIRMED);
    }
}
