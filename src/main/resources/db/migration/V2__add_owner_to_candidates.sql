-- Add owner_id column if it does not exist
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS owner_id BIGINT;

-- Add foreign key constraint if not exists (PostgreSQL requires a check);
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints tc
        WHERE tc.constraint_name = 'fk_candidates_owner'
          AND tc.table_name = 'candidates'
    ) THEN
        ALTER TABLE candidates
            ADD CONSTRAINT fk_candidates_owner
            FOREIGN KEY (owner_id) REFERENCES users(id);
    END IF;
END$$;
