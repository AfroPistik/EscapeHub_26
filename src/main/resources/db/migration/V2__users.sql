
CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL,
                       username      VARCHAR(30)  NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT uq_users_username UNIQUE (username)
);

-- μοναδικό email χωρίς διάκριση πεζών/κεφαλαίων
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));