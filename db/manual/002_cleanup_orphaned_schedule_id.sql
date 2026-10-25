-- scheduleId를 Schedule과의 연관관계로 전환 (이슈 #15)
--
-- 지금까지 account_record.schedule_id 는 외래키가 아닌 단순 숫자 컬럼이어서,
-- 일정을 삭제해도 그 번호가 그대로 남아 있었습니다. 엔티티를 연관관계로 바꾸고
-- @OnDelete(SET_NULL) 을 걸면 앞으로는 일정 삭제 시 DB 레벨에서 자동으로 끊기지만,
-- 이미 쌓인 고아 schedule_id 값이 있으면 Hibernate 가 FK 제약을 거는 과정에서
-- 검증에 실패합니다. 그래서 제약을 걸기 전에 고아 값을 먼저 NULL 처리합니다.
--
-- 실행: 새 버전 배포 직전(권장) 또는 직후, 기존 DB 에 한 번 실행합니다.
--   psql -h <host> -p <port> -U <user> -d <db> -v ON_ERROR_STOP=1 -f db/manual/002_cleanup_orphaned_schedule_id.sql
-- 여러 번 실행해도 결과가 같고, 고아 값이 없는 DB 에서는 아무 것도 하지 않습니다.

BEGIN;

UPDATE account_record ar
SET schedule_id = NULL
WHERE ar.schedule_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM schedule s WHERE s.id = ar.schedule_id
  );

-- 엔티티의 @OnDelete(SET_NULL) 과 같게, 일정이 삭제되어도 기록은 남긴다.
-- 새 버전이 먼저 떠서 Hibernate 가 FK 를 이미 만들었다면 건너뛴다.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint con
        JOIN pg_attribute att
          ON att.attrelid = con.conrelid AND att.attnum = ANY (con.conkey)
        WHERE con.conrelid = 'account_record'::regclass
          AND con.contype = 'f'
          AND att.attname = 'schedule_id'
    ) THEN
        ALTER TABLE account_record
            ADD CONSTRAINT fk_account_record_schedule
            FOREIGN KEY (schedule_id) REFERENCES schedule (id) ON DELETE SET NULL;
    END IF;
END $$;

COMMIT;
