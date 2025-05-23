CREATE TABLE members
(
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    channel_id      VARCHAR(32)                             NOT NULL,
    channel_name    VARCHAR(30)                             NOT NULL,
    nick_name       VARCHAR(30)                             NOT NULL,
    last_login_time TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_members PRIMARY KEY (id)
);

ALTER TABLE members
    ADD CONSTRAINT unq_members_channel_id UNIQUE (channel_id);

CREATE TABLE live_streams
(
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    live_id             BIGINT                                  NOT NULL,
    streamer_id         BIGINT                                  NOT NULL,
    channel_id          VARCHAR(255)                            NOT NULL,
    live_title          VARCHAR(255),
    live_status         VARCHAR(255),
    category_type       VARCHAR(255),
    live_category       VARCHAR(255),
    live_category_value VARCHAR(255),
    open_date           TIMESTAMP WITHOUT TIME ZONE,
    close_date          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_live_streams PRIMARY KEY (id)
);

CREATE INDEX idx_live_streams_channel_id ON live_streams (channel_id);

CREATE UNIQUE INDEX idx_live_streams_live_id_unq ON live_streams (live_id);

CREATE INDEX idx_live_streams_streamer_id ON live_streams (streamer_id);


CREATE TABLE contents_sessions
(
    id                      BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    updated_at              TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    live_id                 BIGINT                                  NOT NULL,
    streamer_id             BIGINT                                  NOT NULL,
    session_code            VARCHAR(255)                            NOT NULL,
    status                  VARCHAR(255)                            NOT NULL,
    game_participation_code VARCHAR(100),
    max_group_participants  INTEGER                                 NOT NULL,
    current_participants    INTEGER                                 NOT NULL,
    CONSTRAINT pk_contents_sessions PRIMARY KEY (id)
);

CREATE INDEX idx_contents_session_code ON contents_sessions (session_code);

CREATE INDEX idx_contents_sessions_live_id ON contents_sessions (live_id);

CREATE INDEX idx_contents_sessions_streamer_id ON contents_sessions (streamer_id);

CREATE TABLE session_participant
(
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    contents_session_id BIGINT                                  NOT NULL,
    viewer_id           BIGINT                                  NOT NULL,
    game_nickname       VARCHAR(255)                            NOT NULL,
    status              VARCHAR(255)                            NOT NULL,
    fixed_pick          BOOLEAN                                 NOT NULL,
    fixed_pick_time     TIMESTAMP WITHOUT TIME ZONE,
    session_round       INTEGER                                 NOT NULL,
    CONSTRAINT pk_session_participant PRIMARY KEY (id)
);

CREATE INDEX idx_participant_session ON session_participant (viewer_id, contents_session_id);

ALTER TABLE session_participant
    ADD CONSTRAINT FK_SESSION_PARTICIPANT_ON_CONTENTS_SESSION FOREIGN KEY (contents_session_id) REFERENCES contents_sessions (id);