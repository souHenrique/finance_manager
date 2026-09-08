CREATE TABLE credit_card_credits (
     id UUID NOT NULL,
     user_id UUID NOT NULL,
     credit_card_id UUID NOT NULL,
     refund_item_id UUID NOT NULL,
     original_amount NUMERIC(19, 2) NOT NULL,
     remaining_amount NUMERIC(19, 2) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
     version BIGINT NOT NULL DEFAULT 0,

     CONSTRAINT pk_credit_card_credits
         PRIMARY KEY (id),

     CONSTRAINT fk_card_credits_user
         FOREIGN KEY (user_id)
             REFERENCES users (id)
             ON DELETE RESTRICT,

     CONSTRAINT fk_card_credits_card
         FOREIGN KEY (credit_card_id)
             REFERENCES credit_cards (id)
             ON DELETE RESTRICT,

     CONSTRAINT fk_card_credits_refund_item
         FOREIGN KEY (refund_item_id)
             REFERENCES credit_card_refund_items (id)
             ON DELETE RESTRICT,

     CONSTRAINT uk_card_credits_refund_item
         UNIQUE (refund_item_id),

     CONSTRAINT ck_card_credits_amounts
         CHECK (
             original_amount > 0
                 AND remaining_amount >= 0
                 AND remaining_amount <= original_amount
             )
);

CREATE TABLE credit_card_credit_applications (
     id UUID NOT NULL,
     credit_id UUID NOT NULL,
     invoice_id UUID NOT NULL,
     amount NUMERIC(19, 2) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,

     CONSTRAINT pk_credit_card_credit_applications
         PRIMARY KEY (id),

     CONSTRAINT fk_credit_applications_credit
         FOREIGN KEY (credit_id)
             REFERENCES credit_card_credits (id)
             ON DELETE RESTRICT,

     CONSTRAINT fk_credit_applications_invoice
         FOREIGN KEY (invoice_id)
             REFERENCES invoices (id)
             ON DELETE RESTRICT,

     CONSTRAINT uk_credit_applications_credit_invoice
         UNIQUE (credit_id, invoice_id),

     CONSTRAINT ck_credit_applications_amount
         CHECK (amount > 0)
);

CREATE INDEX idx_card_credits_available
    ON credit_card_credits (user_id, credit_card_id, created_at, id)
    WHERE remaining_amount > 0;

CREATE INDEX idx_credit_applications_invoice
    ON credit_card_credit_applications (invoice_id);