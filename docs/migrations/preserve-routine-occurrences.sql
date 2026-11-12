-- develop 프로필은 ddl-auto: update로 서버 시작 시 이 컬럼을 자동 추가합니다.
-- 스키마 자동 갱신을 사용하지 않는 환경에서 컬럼이 없을 때만 수동 적용합니다.
ALTER TABLE todo_date
    ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE;
