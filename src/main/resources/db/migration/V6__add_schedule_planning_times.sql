ALTER TABLE schedules
    ADD COLUMN custom_start_at DATETIME;

ALTER TABLE schedules
    ADD COLUMN target_arrival_at DATETIME;
