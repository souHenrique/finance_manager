CREATE TABLE budgets (
     id UUID NOT NULL,
     user_id UUID NOT NULL,
     category_id UUID NOT NULL,
     month INTEGER NOT NULL,
     year INTEGER NOT NULL,
     amount_limit NUMERIC(19, 2) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

     CONSTRAINT pk_budgets
         PRIMARY KEY (id),

     CONSTRAINT fk_budgets_user
         FOREIGN KEY (user_id)
             REFERENCES users(id)
             ON DELETE CASCADE,

     CONSTRAINT fk_budgets_category
         FOREIGN KEY (category_id)
             REFERENCES categories(id)
             ON DELETE RESTRICT,

     CONSTRAINT uk_budgets_user_category_period
         UNIQUE (user_id, category_id, month, year),

     CONSTRAINT ck_budgets_month
         CHECK (month BETWEEN 1 AND 12),

    CONSTRAINT ck_budgets_year
        CHECK (year BETWEEN 1 AND 9999),

    CONSTRAINT ck_budgets_amount_limit_positive
        CHECK (amount_limit > 0)
);

CREATE INDEX idx_budgets_user_period
    ON budgets (user_id, year DESC, month DESC, created_at DESC);
