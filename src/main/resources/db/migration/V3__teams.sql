CREATE TABLE teams (
                       id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       name        VARCHAR(50) NOT NULL,
                       invite_code VARCHAR(8)  NOT NULL,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                       CONSTRAINT uq_teams_invite_code UNIQUE (invite_code)
);

CREATE TABLE team_members (
                              id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              team_id   BIGINT      NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                              user_id   BIGINT      NOT NULL REFERENCES users(id),
                              role      VARCHAR(10) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
                              joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_user_id ON team_members (user_id);
CREATE UNIQUE INDEX uq_team_members_one_owner ON team_members (team_id) WHERE role = 'OWNER';