package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected EventMapper eventMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM registrations");
        jdbcTemplate.update("DELETE FROM events");
        jdbcTemplate.update("DELETE FROM users");
    }

    protected User createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("student123"));
        user.setDisplayName(username);
        user.setRole(role);
        userMapper.insert(user);
        return user;
    }

    protected Event createEvent(Long adminId, int capacity) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Event event = new Event();
        event.setTitle("并发测试活动-" + System.nanoTime());
        event.setDescription("test");
        event.setLocation("test room");
        event.setCategory("test");
        event.setOrganizer("test");
        event.setStartTime(now.plusDays(1));
        event.setEndTime(now.plusDays(1).plusHours(2));
        event.setRegistrationStart(now.minusHours(1));
        event.setRegistrationEnd(now.plusHours(1));
        event.setCapacity(capacity);
        event.setOccupiedCount(0);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCreatedBy(adminId);
        eventMapper.insert(event);
        return event;
    }
}
