package com.campusflow;

import com.campusflow.domain.Event;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.mapper.RegistrationMapper;
import com.campusflow.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Test
    void shouldNotOversellWhenTwentyStudentsRegisterForFiveSeats() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN);
        Event event = createEvent(admin.getId(), 5);

        List<User> students = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            students.add(createUser("student" + index, UserRole.STUDENT));
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (User student : students) {
            futures.add(executor.submit(() -> {
                start.await();
                registrationService.register(event.getId(), student.getId());
                return null;
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        Event persisted = eventMapper.findById(event.getId());
        assertThat(persisted.getOccupiedCount()).isEqualTo(5);
        assertThat(registrationMapper.countByEventAndStatus(event.getId(), RegistrationStatus.CONFIRMED))
                .isEqualTo(5);
        assertThat(registrationMapper.countByEventAndStatus(event.getId(), RegistrationStatus.WAITLISTED))
                .isEqualTo(15);
    }
}
