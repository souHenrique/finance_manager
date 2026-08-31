CREATE TABLE transactions (
      id UUID NOT NULL,
      user_id UUID NOT NULL,
      description VARCHAR(255) NOT NULL,
      amount NUMERIC(19, 2) NOT NULL,
      competence_date DATE NOT NULL,
      effective_date DATE,
      due_date DATE,
      type VARCHAR(40) NOT NULL,
      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
      payment_method VARCHAR(30),
      source_account_id UUID,
      destination_account_id UUID,
      category_id UUID,
      credit_card_id UUID,
      invoice_id UUID,
      installment_group_id UUID,
      installment_number INTEGER,
      installment_count INTEGER,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL,
      updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

      CONSTRAINT pk_transactions
          PRIMARY KEY (id),

      CONSTRAINT fk_transactions_user
          FOREIGN KEY (user_id)
              REFERENCES users (id)
              ON DELETE CASCADE,

      CONSTRAINT fk_transactions_source_account
          FOREIGN KEY (source_account_id)
              REFERENCES accounts (id)
              ON DELETE RESTRICT,

      CONSTRAINT fk_transactions_destination_account
          FOREIGN KEY (destination_account_id)
              REFERENCES accounts (id)
              ON DELETE RESTRICT,

      CONSTRAINT fk_transactions_category
          FOREIGN KEY (category_id)
              REFERENCES categories (id)
              ON DELETE RESTRICT,

      CONSTRAINT ck_transactions_amount_positive
          CHECK (amount > 0),

      CONSTRAINT ck_transactions_type
          CHECK (
              type IN (
                       'INCOME',
                       'EXPENSE',
                       'TRANSFER',
                       'CREDIT_CARD_PURCHASE',
                       'CREDIT_CARD_PAYMENT',
                       'ADJUSTMENT'
                  )
              ),

      CONSTRAINT ck_transactions_status
          CHECK (
              status IN (
                         'PENDING',
                         'COMPLETED',
                         'CANCELLED'
                  )
              ),

      CONSTRAINT ck_transactions_payment_method
          CHECK (
              payment_method IS NULL
                  OR payment_method IN (
                                        'DEBIT',
                                        'PIX',
                                        'CASH',
                                        'TRANSFER',
                                        'CREDIT_CARD',
                                        'OTHER'
                  )
              ),

      CONSTRAINT ck_transactions_installment_number
          CHECK (
              installment_number IS NULL
                  OR installment_number > 0
              ),

      CONSTRAINT ck_transactions_installment_count
          CHECK (
              installment_count IS NULL
                  OR installment_count > 0
              ),

      CONSTRAINT ck_transactions_installment_range
          CHECK (
              installment_number IS NULL
                  OR installment_count IS NULL
                  OR installment_number <= installment_count
              )
);

CREATE INDEX idx_transactions_user_competence_date
    ON transactions (user_id, competence_date);

CREATE INDEX idx_transactions_user_effective_date
    ON transactions (user_id, effective_date);

CREATE INDEX idx_transactions_user_status
    ON transactions (user_id, status);