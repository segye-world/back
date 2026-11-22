ALTER TABLE category ADD COLUMN member_id BIGINT;

-- 공용 카테고리의 소유자는 거래에 기록된 회원으로만 판단할 수 있다.
-- 두 참조에서 같은 공용 카테고리를 사용해도 회원마다 사본을 한 개씩 만든다.
CREATE TEMP TABLE member_category_mapping ON COMMIT DROP AS
SELECT used.member_id, used.category_id AS old_id,
       nextval(pg_get_serial_sequence('category', 'id')) AS new_id
FROM (
    SELECT member_id, category_id FROM account_record
    UNION
    SELECT member_id, source_category_id FROM account_record WHERE source_category_id IS NOT NULL
) AS used;

INSERT INTO category (id, member_id, name, type)
SELECT mapping.new_id, mapping.member_id, category.name, category.type
FROM member_category_mapping AS mapping
JOIN category ON category.id = mapping.old_id;

UPDATE account_record AS record
SET category_id = mapping.new_id
FROM member_category_mapping AS mapping
WHERE record.member_id = mapping.member_id AND record.category_id = mapping.old_id;

UPDATE account_record AS record
SET source_category_id = mapping.new_id
FROM member_category_mapping AS mapping
WHERE record.member_id = mapping.member_id AND record.source_category_id = mapping.old_id;

-- 거래에 쓰이지 않아 소유자를 알 수 없는 공용 항목은 회원 목록으로 이전하지 않는다.
DELETE FROM category WHERE member_id IS NULL;

INSERT INTO category (member_id, name, type)
SELECT member.id, defaults.name, 'INCOME'
FROM member
CROSS JOIN (VALUES ('월급'), ('현금'), ('부수입')) AS defaults(name)
WHERE NOT EXISTS (
    SELECT 1 FROM category
    WHERE category.member_id = member.id AND category.name = defaults.name AND category.type = 'INCOME'
);

ALTER TABLE category
    ALTER COLUMN member_id SET NOT NULL,
    ADD CONSTRAINT fk_category_member FOREIGN KEY (member_id) REFERENCES member (id),
    ADD CONSTRAINT uq_category_member_id UNIQUE (member_id, id);

ALTER TABLE account_record
    DROP CONSTRAINT fk_account_record_category,
    DROP CONSTRAINT fk_account_record_source_category,
    ADD CONSTRAINT fk_account_record_category
        FOREIGN KEY (member_id, category_id) REFERENCES category (member_id, id),
    ADD CONSTRAINT fk_account_record_source_category
        FOREIGN KEY (member_id, source_category_id) REFERENCES category (member_id, id)
        ON DELETE SET NULL (source_category_id);

CREATE INDEX idx_account_record_category ON account_record (category_id);
CREATE INDEX idx_account_record_source_category ON account_record (source_category_id);
