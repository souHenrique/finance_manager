CREATE TABLE credit_card_refunds (
     id UUID NOT NULL,
     user_id UUID NOT NULL,
     credit_card_id UUID NOT NULL,
     selected_transaction_id UUID NOT NULL,
     installment_group_id UUID,
     reason VARCHAR(500) NOT NULL,
     total_amount NUMERIC(19, 2) NOT NULL,
     limit_restored_amount NUMERIC(19, 2) NOT NULL,
     paid_compensation_amount NUMERIC(19, 2) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,

     CONSTRAINT pk_credit_card_refunds
         PRIMARY KEY (id),

     CONSTRAINT fk_refunds_user
         FOREIGN KEY (user_id)
             REFERENCES users (id)
             ON DELETE RESTRICT,

     CONSTRAINT fk_refunds_card
         FOREIGN KEY (credit_card_id)
             REFERENCES credit_cards (id)
             ON DELETE RESTRICT,

     CONSTRAINT fk_refunds_selected_transaction
         FOREIGN KEY (selected_transaction_id)
             REFERENCES transactions (id)
             ON DELETE RESTRICT,

     CONSTRAINT ck_refunds_reason
         CHECK (length(trim(reason)) > 0),

     CONSTRAINT ck_refunds_amounts
         CHECK (
             total_amount > 0
                 AND limit_restored_amount >= 0
                 AND paid_compensation_amount >= 0
                 AND total_amount =
                     limit_restored_amount + paid_compensation_amount
             )
);

CREATE INDEX idx_refunds_user_card_created
    ON credit_card_refunds (
            user_id,
            credit_card_id,
            created_at
        );


CREATE TABLE credit_card_refund_items (
    id UUID NOT NULL,
    refund_id UUID NOT NULL,
    original_transaction_id UUID NOT NULL,
    original_invoice_id UUID NOT NULL,
    original_invoice_status VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    treatment VARCHAR(40) NOT NULL,

    CONSTRAINT pk_credit_card_refund_items
      PRIMARY KEY (id),

    CONSTRAINT fk_refund_items_refund
      FOREIGN KEY (refund_id)
          REFERENCES credit_card_refunds (id)
          ON DELETE RESTRICT,

    CONSTRAINT fk_refund_items_transaction
      FOREIGN KEY (original_transaction_id)
          REFERENCES transactions (id)
          ON DELETE RESTRICT,

    CONSTRAINT fk_refund_items_invoice
      FOREIGN KEY (original_invoice_id)
          REFERENCES invoices (id)
          ON DELETE RESTRICT,

    CONSTRAINT uk_refund_items_original_transaction
      UNIQUE (original_transaction_id),

    CONSTRAINT ck_refund_items_amount
      CHECK (amount > 0),

    CONSTRAINT ck_refund_items_treatment_status
      CHECK (
          (
              treatment = 'UNPAID_CANCELLATION'
                  AND original_invoice_status IN ('OPEN', 'CLOSED')
              )
              OR
          (
              treatment = 'FUTURE_INVOICE_CREDIT'
                  AND original_invoice_status = 'PAID'
              )
          )
);

CREATE INDEX idx_refund_items_refund
    ON credit_card_refund_items (refund_id);