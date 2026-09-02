CREATE SEQUENCE audit_revision_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE audit_revision (
    rev INTEGER NOT NULL,
    revtstmp BIGINT NOT NULL,

    CONSTRAINT pk_audit_revision
        PRIMARY KEY (rev)
    );

    CREATE TABLE accounts_aud (
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    id UUID NOT NULL,

    user_id UUID,
    name VARCHAR(120),
    type VARCHAR(30),
    institution VARCHAR(160),
    initial_balance NUMERIC(19, 2),
    current_balance NUMERIC(19, 2),
    status VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_accounts_aud
      PRIMARY KEY (rev, id),

    CONSTRAINT fk_accounts_aud_revision
      FOREIGN KEY (rev)
          REFERENCES audit_revision (rev)
    );

CREATE INDEX idx_accounts_aud_entity_revision
    ON accounts_aud (id, rev);

CREATE TABLE transactions_aud (
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    id UUID NOT NULL,

    user_id UUID,
    description VARCHAR(255),
    amount NUMERIC(19, 2),
    competence_date DATE,
    effective_date DATE,
    due_date DATE,
    type VARCHAR(40),
    status VARCHAR(20),
    payment_method VARCHAR(30),
    source_account_id UUID,
    destination_account_id UUID,
    category_id UUID,
    credit_card_id UUID,
    invoice_id UUID,
    installment_group_id UUID,
    installment_number INTEGER,
    installment_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_transactions_aud
      PRIMARY KEY (rev, id),

    CONSTRAINT fk_transactions_aud_revision
      FOREIGN KEY (rev)
          REFERENCES audit_revision (rev)
    );

CREATE INDEX idx_transactions_aud_entity_revision
    ON transactions_aud (id, rev);