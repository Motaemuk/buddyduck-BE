CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kakao_id VARCHAR(100),
    nickname VARCHAR(30) NOT NULL,
    age_range VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    gender VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    avatar_color VARCHAR(20) NOT NULL DEFAULT '#FACC15',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_kakao_id UNIQUE (kakao_id)
);

CREATE TABLE concerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    venue_name VARCHAR(200) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME,
    lat DECIMAL(10, 7) NOT NULL,
    lng DECIMAL(10, 7) NOT NULL,
    source VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_concerts_source_external_id UNIQUE (source, external_id)
);

CREATE INDEX idx_concerts_start_at ON concerts (start_at);
CREATE INDEX idx_concerts_title ON concerts (title);

CREATE TABLE concert_interest_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    concert_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_concert_interest_tags_user_concert_tag UNIQUE (user_id, concert_id, tag),
    CONSTRAINT fk_concert_interest_tags_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_concert_interest_tags_concert FOREIGN KEY (concert_id) REFERENCES concerts (id)
);

CREATE INDEX idx_concert_interest_tags_concert_tag ON concert_interest_tags (concert_id, tag);

CREATE TABLE places (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    provider_place_id VARCHAR(100),
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300) NOT NULL,
    lat DECIMAL(10, 7) NOT NULL,
    lng DECIMAL(10, 7) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_places_provider_place_id UNIQUE (provider, provider_place_id)
);

CREATE INDEX idx_places_lat_lng ON places (lat, lng);

CREATE TABLE rooms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    concert_id BIGINT NOT NULL,
    host_user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    max_members INT NOT NULL,
    meeting_at DATETIME NOT NULL,
    meeting_place_id BIGINT NOT NULL,
    event_place_id BIGINT NOT NULL,
    open_chat_url VARCHAR(500) NOT NULL,
    open_chat_password VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rooms_concert FOREIGN KEY (concert_id) REFERENCES concerts (id),
    CONSTRAINT fk_rooms_host_user FOREIGN KEY (host_user_id) REFERENCES users (id),
    CONSTRAINT fk_rooms_meeting_place FOREIGN KEY (meeting_place_id) REFERENCES places (id),
    CONSTRAINT fk_rooms_event_place FOREIGN KEY (event_place_id) REFERENCES places (id)
);

CREATE INDEX idx_rooms_concert_status ON rooms (concert_id, status);
CREATE INDEX idx_rooms_host_user ON rooms (host_user_id);

CREATE TABLE room_tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_room_tags_room_tag UNIQUE (room_id, tag),
    CONSTRAINT fk_room_tags_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE INDEX idx_room_tags_tag ON room_tags (tag);

CREATE TABLE room_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_room_members_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_room_members_user ON room_members (user_id);

CREATE TABLE join_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by BIGINT,
    decided_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_join_requests_room_user UNIQUE (room_id, user_id),
    CONSTRAINT fk_join_requests_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_join_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_join_requests_decided_by FOREIGN KEY (decided_by) REFERENCES users (id)
);

CREATE INDEX idx_join_requests_room_status ON join_requests (room_id, status);
CREATE INDEX idx_join_requests_user_status ON join_requests (user_id, status);

CREATE TABLE schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    arrival_buffer_minutes INT NOT NULL DEFAULT 30,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_schedules_room UNIQUE (room_id),
    CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

CREATE TABLE schedule_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    place_id BIGINT,
    slot_type VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    dwell_minutes INT NOT NULL,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_schedule_slots_schedule_id UNIQUE (schedule_id, id),
    CONSTRAINT uk_schedule_slots_schedule_sort_order UNIQUE (schedule_id, sort_order),
    CONSTRAINT fk_schedule_slots_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT fk_schedule_slots_place FOREIGN KEY (place_id) REFERENCES places (id)
);

CREATE INDEX idx_schedule_slots_place ON schedule_slots (place_id);

CREATE TABLE route_segments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    from_slot_id BIGINT NOT NULL,
    to_slot_id BIGINT NOT NULL,
    mode VARCHAR(20) NOT NULL,
    distance_meters INT,
    duration_minutes INT NOT NULL,
    provider VARCHAR(30),
    manually_adjusted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_route_segments_schedule_from_to UNIQUE (schedule_id, from_slot_id, to_slot_id),
    CONSTRAINT fk_route_segments_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    CONSTRAINT fk_route_segments_from_slot FOREIGN KEY (schedule_id, from_slot_id)
        REFERENCES schedule_slots (schedule_id, id),
    CONSTRAINT fk_route_segments_to_slot FOREIGN KEY (schedule_id, to_slot_id)
        REFERENCES schedule_slots (schedule_id, id)
);
