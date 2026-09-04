CREATE TABLE invoices (
    id UUID NOT NULL,
    credit_card_id UUID NOT NULL,
    reference_month INTEGER NOT NULL,
    reference_year INTEGER NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    paid_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_invoices
      PRIMARY KEY (id),

    CONSTRAINT fk_invoices_credit_card
      FOREIGN KEY (credit_card_id)
          REFERENCES credit_cards (id)
          ON DELETE RESTRICT,

    CONSTRAINT uk_invoices_card_reference
      UNIQUE (
              credit_card_id,
              reference_month,
              reference_year
          ),

    CONSTRAINT ck_invoices_reference_month
      CHECK (reference_month BETWEEN 1 AND 12),

    CONSTRAINT ck_invoices_reference_year
      CHECK (reference_year > 0),

    CONSTRAINT ck_invoices_total_amount
      CHECK (total_amount >= 0),

    CONSTRAINT ck_invoices_status
      CHECK (
          status IN (
                     'OPEN',
                     'CLOSED',
                     'PAID',
                     'CANCELLED'
              )
          ),

    CONSTRAINT ck_invoices_dates
      CHECK (due_date > closing_date)
);

CREATE TABLE invoices_aud (
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    id UUID NOT NULL,

    credit_card_id UUID,
    reference_month INTEGER,
    reference_year INTEGER,
    closing_date DATE,
    due_date DATE,
    total_amount NUMERIC(19, 2),
    status VARCHAR(20),
    paid_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_invoices_aud
      PRIMARY KEY (rev, id),

    CONSTRAINT fk_invoices_aud_revision
      FOREIGN KEY (rev)
          REFERENCES audit_revision (rev)
);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_invoice
        FOREIGN KEY (invoice_id)
            REFERENCES invoices (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_invoices_card_status
    ON invoices (credit_card_id, status);

CREATE INDEX idx_invoices_aud_entity_revision
    ON invoices_aud (id, rev);

CREATE INDEX idx_transactions_invoice_id
    ON transactions (invoice_id);