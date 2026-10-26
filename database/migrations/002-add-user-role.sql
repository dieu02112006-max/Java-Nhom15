-- Apply once to databases created before role-based authorization was added.
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER skin_id;
