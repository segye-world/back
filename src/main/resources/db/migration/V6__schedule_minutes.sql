-- 일정 시작·종료 시간을 시 단위에서 분 단위로 변경 (이슈 #27)
-- 값은 하루 시작(00:00)부터의 분이다. 기존 시 값은 * 60 으로 옮긴다. (14시 → 840, 24시 → 1440)

ALTER TABLE schedule RENAME COLUMN start_hour TO start_minute;
ALTER TABLE schedule RENAME COLUMN end_hour TO end_minute;

UPDATE schedule SET start_minute = start_minute * 60, end_minute = end_minute * 60;

ALTER TABLE schedule ADD CONSTRAINT schedule_minute_range_check
    CHECK (start_minute >= 0 AND end_minute <= 1440 AND start_minute < end_minute);
