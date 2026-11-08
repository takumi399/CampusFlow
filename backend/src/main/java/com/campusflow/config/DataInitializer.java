package com.campusflow.config;

import com.campusflow.domain.Event;
import com.campusflow.domain.EventStatus;
import com.campusflow.domain.Registration;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.User;
import com.campusflow.domain.UserRole;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.RegistrationMapper;
import com.campusflow.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SeedProperties seedProperties;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SeedProperties seedProperties,
            UserMapper userMapper,
            EventMapper eventMapper,
            RegistrationMapper registrationMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.seedProperties = seedProperties;
        this.userMapper = userMapper;
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedProperties.enabled()) {
            return;
        }

        if (userMapper.count() == 0) {
            seedUsers();
        }

        if (eventMapper.count() == 0) {
            seedEvents();
        }
    }

    private void seedUsers() {
        User admin = createUser("admin", "admin123", "活动管理员", UserRole.ADMIN);
        createUser("student1", "student123", "林晓", UserRole.STUDENT);
        createUser("student2", "student123", "周宁", UserRole.STUDENT);
        createUser("student3", "student123", "陈一", UserRole.STUDENT);
        log.info("Seeded demo users, admin id={}", admin.getId());
    }

    private User createUser(String username, String rawPassword, String displayName, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(role);
        userMapper.insert(user);
        return user;
    }

    private void seedEvents() {
        User admin = userMapper.findByUsername("admin");
        if (admin == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now().withNano(0);

        Event music = createEvent(
                admin.getId(),
                "秋日校园音乐节",
                "校园乐队与社团联合演出，凭报名记录入场。",
                "东区草坪",
                "文艺",
                "校学生会",
                now.plusDays(7).withHour(19).withMinute(0),
                now.plusDays(7).withHour(21).withMinute(30),
                now.minusDays(1),
                now.plusDays(5).withHour(23).withMinute(59),
                200,
                EventStatus.PUBLISHED
        );

        createEvent(
                admin.getId(),
                "AI 应用开发工作坊",
                "从需求拆解到可运行 Demo，完成一次小型 AI 应用开发练习。",
                "创新楼 302",
                "技术",
                "计算机学院学生会",
                now.plusDays(10).withHour(14).withMinute(0),
                now.plusDays(10).withHour(17).withMinute(0),
                now.minusDays(1),
                now.plusDays(8).withHour(23).withMinute(59),
                30,
                EventStatus.PUBLISHED
        );

        createEvent(
                admin.getId(),
                "周末羽毛球友谊赛",
                "随机组队双打，现场提供基础器材。",
                "体育馆二号场",
                "体育",
                "体育部",
                now.plusDays(3).withHour(18).withMinute(0),
                now.plusDays(3).withHour(20).withMinute(0),
                now.minusDays(2),
                now.plusDays(2).withHour(23).withMinute(59),
                12,
                EventStatus.PUBLISHED
        );

        createEvent(
                admin.getId(),
                "志愿者招募说明会",
                "介绍本学期校园志愿服务项目与报名方式。",
                "学生活动中心 201",
                "公益",
                "青年志愿者协会",
                now.plusDays(20).withHour(19).withMinute(0),
                now.plusDays(20).withHour(20).withMinute(30),
                now.plusDays(1),
                now.plusDays(15).withHour(23).withMinute(59),
                50,
                EventStatus.DRAFT
        );

        Event photo = createEvent(
                admin.getId(),
                "校园摄影分享会",
                "三位校园摄影师分享拍摄思路，名额有限。",
                "图书馆报告厅",
                "兴趣",
                "摄影协会",
                now.plusDays(5).withHour(19).withMinute(0),
                now.plusDays(5).withHour(20).withMinute(30),
                now.minusDays(1),
                now.plusDays(4).withHour(23).withMinute(59),
                2,
                EventStatus.PUBLISHED
        );

        seedPhotoRegistrations(photo);
        log.info("Seeded demo events, featured event id={}", music.getId());
    }

    private Event createEvent(
            Long createdBy,
            String title,
            String description,
            String location,
            String category,
            String organizer,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            int capacity,
            EventStatus status
    ) {
        Event event = new Event();
        event.setCreatedBy(createdBy);
        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setCategory(category);
        event.setOrganizer(organizer);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setRegistrationStart(registrationStart);
        event.setRegistrationEnd(registrationEnd);
        event.setCapacity(capacity);
        event.setOccupiedCount(0);
        event.setStatus(status);
        eventMapper.insert(event);
        return event;
    }

    private void seedPhotoRegistrations(Event event) {
        User student1 = userMapper.findByUsername("student1");
        User student2 = userMapper.findByUsername("student2");
        User student3 = userMapper.findByUsername("student3");
        if (student1 == null || student2 == null || student3 == null) {
            return;
        }

        insertRegistration(event.getId(), student1.getId(), RegistrationStatus.CONFIRMED, null);
        insertRegistration(event.getId(), student2.getId(), RegistrationStatus.CONFIRMED, null);
        insertRegistration(event.getId(), student3.getId(), RegistrationStatus.WAITLISTED, LocalDateTime.now());
        eventMapper.incrementOccupied(event.getId());
        eventMapper.incrementOccupied(event.getId());
    }

    private void insertRegistration(
            Long eventId,
            Long userId,
            RegistrationStatus status,
            LocalDateTime waitlistTime
    ) {
        Registration registration = new Registration();
        registration.setEventId(eventId);
        registration.setUserId(userId);
        registration.setStatus(status);
        registration.setWaitlistTime(waitlistTime);
        registrationMapper.insert(registration);
    }
}
