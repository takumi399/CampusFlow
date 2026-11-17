CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NULL,
    location VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    organizer VARCHAR(100) NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    registration_start DATETIME(3) NOT NULL,
    registration_end DATETIME(3) NOT NULL,
    capacity INT NOT NULL,
    occupied_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_events_status_start (status, start_time),
    KEY idx_events_created_by (created_by),
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_events_capacity CHECK (capacity > 0),
    CONSTRAINT chk_events_occupied CHECK (occupied_count >= 0 AND occupied_count <= capacity),
    CONSTRAINT chk_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELED', 'FINISHED'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    waitlist_time DATETIME(3) NULL,
    confirm_deadline DATETIME(3) NULL,
    checked_in_at DATETIME(3) NULL,
    canceled_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_registrations_event_user (event_id, user_id),
    KEY idx_registrations_event_status_waitlist (event_id, status, waitlist_time),
    KEY idx_registrations_user_status (user_id, status),
    KEY idx_registrations_promotion_deadline (status, confirm_deadline),
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_registrations_status CHECK (
        status IN ('CONFIRMED', 'WAITLISTED', 'PROMOTED', 'CANCELED', 'EXPIRED', 'CHECKED_IN')
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
