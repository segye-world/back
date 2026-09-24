-- 지출 수단(payment_method) → 수입원 카테고리(category, type=INCOME) 전환 (PR #11, 이슈 #9)
--
-- 아직 Flyway 도입 전(ddl-auto: update)이라 수동으로 실행하는 스크립트입니다.
-- ddl-auto: update 는 컬럼 값을 옮기지도, 옛 컬럼/테이블을 지우지도 않기 때문에
--   1) 기존 기록의 수단 연결이 NULL 이 되고
--   2) 남은 payment_method.member_id FK 때문에 회원 탈퇴가 실패합니다.
-- 이를 막기 위해 값을 옮긴 뒤 옛 컬럼과 테이블을 정리합니다.
--
-- 실행: 새 버전 배포 직전(권장) 또는 직후, 기존 DB 에 한 번 실행합니다.
--   psql -h <host> -p <port> -U <user> -d <db> -v ON_ERROR_STOP=1 -f db/manual/001_payment_method_to_source_category.sql
-- 여러 번 실행해도 결과가 같고, 새로 만든 DB 에서는 아무 것도 하지 않습니다.

BEGIN;

ALTER TABLE account_record ADD COLUMN IF NOT EXISTS source_category_id BIGINT;

DO $$
BEGIN
    IF to_regclass('payment_method') IS NULL THEN
        RAISE NOTICE 'payment_method 테이블이 없어 값 이관을 건너뜁니다.';
        RETURN;
    END IF;

    -- 기록이 실제로 참조하는 수단 이름 중 같은 이름의 INCOME 카테고리가 없는 것만 만든다.
    -- 카테고리는 전역이므로, 쓰이지 않던 회원별 수단까지 모두에게 노출하지 않는다.
    INSERT INTO category (name, type)
    SELECT DISTINCT pm.name, 'INCOME'
    FROM account_record ar
    JOIN payment_method pm ON pm.id = ar.payment_method_id
    WHERE NOT EXISTS (
        SELECT 1 FROM category c WHERE c.name = pm.name AND c.type = 'INCOME'
    );

    -- 같은 이름의 INCOME 카테고리가 여러 개면 가장 먼저 만든 것에 붙인다.
    UPDATE account_record ar
    SET source_category_id = (
        SELECT MIN(c.id) FROM category c WHERE c.name = pm.name AND c.type = 'INCOME'
    )
    FROM payment_method pm
    WHERE pm.id = ar.payment_method_id
      AND ar.source_category_id IS NULL;
END $$;

-- 컬럼을 지우면 그 컬럼에 걸린 FK 도 함께 사라진다.
ALTER TABLE account_record DROP COLUMN IF EXISTS payment_method_id;
DROP TABLE IF EXISTS payment_method;

-- 엔티티의 @OnDelete(SET_NULL) 과 같게, 수입원이 삭제되어도 기록은 남긴다.
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
          AND att.attname = 'source_category_id'
    ) THEN
        ALTER TABLE account_record
            ADD CONSTRAINT fk_account_record_source_category
            FOREIGN KEY (source_category_id) REFERENCES category (id) ON DELETE SET NULL;
    END IF;
END $$;

COMMIT;
