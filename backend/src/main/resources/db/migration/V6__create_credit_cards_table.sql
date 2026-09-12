CREATE TABLE credit_cards (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    credit_limit NUMERIC(19, 2) NOT NULL,
    available_limit NUMERIC(19, 2) NOT NULL,
    closing_day INTEGER NOT NULL,
    due_day INTEGER NOT NULL,
    default_account_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_credit_cards
      PRIMARY KEY (id),

    CONSTRAINT fk_credit_cards_user
      FOREIGN KEY (user_id)
          REFERENCES users (id)
          ON DELETE CASCADE,

    CONSTRAINT fk_credit_cards_default_account
      FOREIGN KEY (default_account_id)
          REFERENCES accounts (id)
          ON DELETE RESTRICT,

    CONSTRAINT ck_credit_cards_name
      CHECK (BTRIM(name) <> ''),

    CONSTRAINT ck_credit_cards_credit_limit
      CHECK (credit_limit > 0),

    CONSTRAINT ck_credit_cards_available_limit
      CHECK (
          available_limit >= 0
              AND available_limit <= credit_limit
          ),

    CONSTRAINT ck_credit_cards_closing_day
      CHECK (closing_day BETWEEN 1 AND 31),

    CONSTRAINT ck_credit_cards_due_day
      CHECK (due_day BETWEEN 1 AND 31),

    CONSTRAINT ck_credit_cards_status
      CHECK (
          status IN (
                     'ACTIVE',
                     'INACTIVE',
                     'BLOCKED'
              )
          )
);

CREATE TABLE credit_cards_aud (
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    id UUID NOT NULL,

    user_id UUID,
    name VARCHAR(120),
    credit_limit NUMERIC(19, 2),
    available_limit NUMERIC(19, 2),
    closing_day INTEGER,
    due_day INTEGER,
    default_account_id UUID,
    status VARCHAR(20),

    CONSTRAINT pk_credit_cards_aud
      PRIMARY KEY (rev, id),

    CONSTRAINT fk_credit_cards_aud_revision
      FOREIGN KEY (rev)
          REFERENCES audit_revision (rev)
);


CREATE INDEX idx_credit_cards_user_id
    ON credit_cards (user_id);

CREATE INDEX idx_credit_cards_user_status
    ON credit_cards (user_id, status);

CREATE INDEX idx_credit_cards_default_account
    ON credit_cards (default_account_id);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_credit_card
        FOREIGN KEY (credit_card_id)
            REFERENCES credit_cards (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_transactions_credit_card_id
    ON transactions (credit_card_id);

CREATE INDEX idx_credit_cards_aud_entity_revision
    ON credit_cards_aud (id, rev);