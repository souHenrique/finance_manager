CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id UUID NOT NULL,
                          name VARCHAR(100) NOT NULL,
                          type VARCHAR(50) NOT NULL,
                          bank_name VARCHAR(100),
                          currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
                          initial_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                          current_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                          color VARCHAR(20) DEFAULT '#3B82F6',
                          icon VARCHAR(50) DEFAULT 'account_balance',
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          version INTEGER NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id UUID NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            type VARCHAR(50) NOT NULL,
                            color VARCHAR(20) DEFAULT '#6B7280',
                            icon VARCHAR(50) DEFAULT 'category',
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE subcategories (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               category_id UUID NOT NULL,
                               name VARCHAR(100) NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_subcategories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE credit_cards (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL,
                              name VARCHAR(100) NOT NULL,
                              brand VARCHAR(50),
                              last_four_digits VARCHAR(4),
                              credit_limit DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                              available_limit DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                              closing_day INT NOT NULL,
                              due_day INT NOT NULL,
                              color VARCHAR(20) DEFAULT '#111827',
                              default_account_id UUID,
                              active BOOLEAN NOT NULL DEFAULT TRUE,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_credit_cards_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                              CONSTRAINT fk_credit_cards_default_account FOREIGN KEY (default_account_id) REFERENCES accounts(id) ON DELETE SET NULL
);

CREATE TABLE invoices (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          credit_card_id UUID NOT NULL,
                          month INT NOT NULL,
                          year INT NOT NULL,
                          closing_date DATE NOT NULL,
                          due_date DATE NOT NULL,
                          total_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                          paid_amount DECIMAL(19,4) DEFAULT 0.0000,
                          payment_date TIMESTAMP WITH TIME ZONE,
                          status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_invoices_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id) ON DELETE CASCADE
);

CREATE TABLE investments (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id UUID NOT NULL,
                             name VARCHAR(100) NOT NULL,
                             type VARCHAR(50) NOT NULL,
                             institution VARCHAR(100) NOT NULL,
                             initial_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                             current_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                             status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_investments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE budgets (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id UUID NOT NULL,
                         category_id UUID NOT NULL,
                         month INT NOT NULL,
                         year INT NOT NULL,
                         amount_limit DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL,
                              description VARCHAR(255) NOT NULL,
                              amount DECIMAL(19,4) NOT NULL,
                              date TIMESTAMP WITH TIME ZONE NOT NULL,
                              due_date TIMESTAMP WITH TIME ZONE,
                              type VARCHAR(50) NOT NULL,
                              payment_method VARCHAR(50) NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              installment_number INT DEFAULT 1,
                              total_installments INT DEFAULT 1,
                              receipt_url VARCHAR(500),
                              notes TEXT,
                              source_account_id UUID,
                              destination_account_id UUID,
                              category_id UUID,
                              subcategory_id UUID,
                              credit_card_id UUID,
                              invoice_id UUID,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                              CONSTRAINT fk_transactions_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
                              CONSTRAINT fk_transactions_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
                              CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
                              CONSTRAINT fk_transactions_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories(id) ON DELETE SET NULL,
                              CONSTRAINT fk_transactions_credit_card FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id) ON DELETE SET NULL,
                              CONSTRAINT fk_transactions_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
);

CREATE INDEX idx_transactions_user_date ON transactions(user_id, date);
CREATE INDEX idx_transactions_category ON transactions(category_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_invoices_card_status ON invoices(credit_card_id, status);
CREATE INDEX idx_budgets_user_period ON budgets(user_id, year, month);