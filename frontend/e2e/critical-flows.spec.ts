import { expect, Page, Route, test } from '@playwright/test';

type JsonObject = Record<string, unknown>;

const NOW = '2026-09-17T12:00:00Z';
const SESSION_STORAGE_KEY = 'nummo.session';

const primaryAccount = {
  id: 'account-primary',
  name: 'Conta Walter',
  type: 'CHECKING',
  institution: 'Banco Albuquerque',
  initialBalance: 1000,
  currentBalance: 1200,
  status: 'ACTIVE',
  version: 1,
  createdAt: NOW,
  updatedAt: NOW,
};

const secondaryAccount = {
  id: 'account-secondary',
  name: 'Conta Jesse',
  type: 'SAVINGS',
  institution: 'Banco Azul',
  initialBalance: 500,
  currentBalance: 500,
  status: 'ACTIVE',
  version: 1,
  createdAt: NOW,
  updatedAt: NOW,
};

const expenseCategory = {
  id: 'category-expense',
  name: 'Mercado',
  type: 'EXPENSE',
  parentCategoryId: null,
  status: 'ACTIVE',
  createdAt: NOW,
  updatedAt: NOW,
};

const incomeCategory = {
  id: 'category-income',
  name: 'Salário',
  type: 'INCOME',
  parentCategoryId: null,
  status: 'ACTIVE',
  createdAt: NOW,
  updatedAt: NOW,
};

const creditCard = {
  id: 'card-e2e',
  name: 'Cartão Heisenberg',
  creditLimit: 5000,
  availableLimit: 4600,
  closingDay: 10,
  dueDay: 17,
  defaultAccountId: primaryAccount.id,
  status: 'ACTIVE',
  version: 1,
};

function transaction(overrides: JsonObject = {}): JsonObject {
  return {
    id: 'transaction-e2e',
    description: 'Lançamento de teste',
    amount: 100,
    competenceDate: '2026-09-17',
    effectiveDate: '2026-09-17',
    dueDate: null,
    type: 'EXPENSE',
    status: 'COMPLETED',
    paymentMethod: 'PIX',
    sourceAccountId: primaryAccount.id,
    destinationAccountId: null,
    categoryId: expenseCategory.id,
    creditCardId: null,
    invoiceId: null,
    installmentGroupId: null,
    installmentNumber: null,
    installmentCount: null,
    createdAt: NOW,
    updatedAt: NOW,
    ...overrides,
  };
}

function pageResponse(content: JsonObject[] = []): JsonObject {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
  };
}

function dashboard(): JsonObject {
  const cash = { basis: 'CASH', amount: 1200 };

  return {
    referenceDate: '2026-09-17',
    year: 2026,
    month: 9,
    periodStart: '2026-09-01',
    periodEnd: '2026-09-30',
    monthlyBalance: { basis: 'CASH_AND_INVOICE', amount: 900 },
    monthlyInflows: cash,
    totalOutflows: { basis: 'CASH', amount: 300 },
    monthlyOutflows: { basis: 'CASH', amount: 200 },
    creditCardPurchaseOutflows: { basis: 'COMPETENCE', amount: 100 },
    competenceExpenses: { basis: 'COMPETENCE', amount: 300 },
    openInvoices: { basis: 'CASH', amount: 100 },
    budget: {
      basis: 'COMPETENCE',
      totalLimit: 1000,
      totalSpent: 300,
      usagePercentage: 30,
      items: [],
    },
    consolidatedBalance: cash,
  };
}

async function fulfillJson(route: Route, body: unknown, status = 200): Promise<void> {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  });
}

async function addAuthenticatedSession(page: Page): Promise<void> {
  await page.addInitScript((storageKey) => {
    sessionStorage.setItem(
      storageKey,
      JSON.stringify({
        token: 'e2e-token',
        tokenType: 'Bearer',
        expiresAt: Date.now() + 60 * 60 * 1000,
      }),
    );
  }, SESSION_STORAGE_KEY);
}

async function installFinanceApiMock(page: Page): Promise<void> {
  const accounts: JsonObject[] = [{ ...primaryAccount }, { ...secondaryAccount }];
  const categories: JsonObject[] = [{ ...expenseCategory }, { ...incomeCategory }];
  const budgets: JsonObject[] = [];
  let invoiceStatus = 'OPEN';

  await page.route('**/api/v1/**', async (route) => {
    const request = route.request();
    const method = request.method();
    const url = new URL(request.url());
    const path = url.pathname;

    if (method === 'POST' && path === '/api/v1/auth/register') {
      const body = request.postDataJSON() as JsonObject;

      await fulfillJson(route, {
        id: 'user-e2e',
        name: body['name'],
        email: body['email'],
        createdAt: NOW,
        updatedAt: NOW,
      });
      return;
    }

    if (method === 'POST' && path === '/api/v1/auth/login') {
      await fulfillJson(route, {
        token: 'e2e-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      });
      return;
    }

    if (method === 'GET' && path === '/api/v1/dashboard') {
      await fulfillJson(route, dashboard());
      return;
    }

    if (path === '/api/v1/accounts' && method === 'GET') {
      await fulfillJson(route, accounts);
      return;
    }

    if (path === '/api/v1/accounts' && method === 'POST') {
      const body = request.postDataJSON() as JsonObject;
      const createdAccount = {
        ...primaryAccount,
        ...body,
        id: 'account-e2e',
        currentBalance: body['initialBalance'],
      };

      accounts.push(createdAccount);
      await fulfillJson(route, createdAccount, 201);
      return;
    }

    if (method === 'GET' && path.startsWith('/api/v1/accounts/')) {
      const id = path.split('/').at(-1);
      await fulfillJson(route, accounts.find((account) => account['id'] === id) ?? primaryAccount);
      return;
    }

    if (path === '/api/v1/categories' && method === 'GET') {
      await fulfillJson(route, categories);
      return;
    }

    if (path === '/api/v1/categories' && method === 'POST') {
      const body = request.postDataJSON() as JsonObject;
      const createdCategory = {
        ...expenseCategory,
        ...body,
        id: 'category-e2e',
        parentCategoryId: body['parentCategoryId'] ?? null,
      };

      categories.push(createdCategory);
      await fulfillJson(route, createdCategory, 201);
      return;
    }

    if (path === '/api/v1/credit-cards' && method === 'GET') {
      await fulfillJson(route, [creditCard]);
      return;
    }

    if (method === 'GET' && path === `/api/v1/credit-cards/${creditCard.id}`) {
      await fulfillJson(route, creditCard);
      return;
    }

    if (method === 'POST' && path === `/api/v1/credit-cards/${creditCard.id}/purchases`) {
      const body = request.postDataJSON() as JsonObject;
      await fulfillJson(
        route,
        [
          transaction({
            id: 'card-purchase-e2e',
            description: body['description'],
            amount: body['amount'],
            competenceDate: body['purchaseDate'],
            effectiveDate: body['purchaseDate'],
            type: 'CREDIT_CARD_PURCHASE',
            paymentMethod: 'CREDIT_CARD',
            sourceAccountId: null,
            categoryId: body['categoryId'],
            creditCardId: creditCard.id,
            invoiceId: 'invoice-e2e',
            installmentNumber: 1,
            installmentCount: body['installmentCount'],
          }),
        ],
        201,
      );
      return;
    }

    if (path === '/api/v1/transactions' && method === 'GET') {
      await fulfillJson(route, pageResponse());
      return;
    }

    if (path === '/api/v1/transactions' && method === 'POST') {
      const body = request.postDataJSON() as JsonObject;
      await fulfillJson(route, transaction({ ...body, id: 'transaction-e2e' }), 201);
      return;
    }

    if (method === 'POST' && path === '/api/v1/transfers') {
      const body = request.postDataJSON() as JsonObject;
      await fulfillJson(
        route,
        transaction({
          id: 'transfer-e2e',
          description: body['description'],
          amount: body['amount'],
          competenceDate: body['date'],
          effectiveDate: body['date'],
          type: 'TRANSFER',
          paymentMethod: 'TRANSFER',
          sourceAccountId: body['sourceAccountId'],
          destinationAccountId: body['destinationAccountId'],
          categoryId: null,
        }),
        201,
      );
      return;
    }

    if (method === 'GET' && path.startsWith('/api/v1/transactions/')) {
      const id = path.split('/').at(-1);
      await fulfillJson(route, transaction({ id }));
      return;
    }

    if (method === 'GET' && path === '/api/v1/invoices/invoice-e2e') {
      await fulfillJson(route, {
        id: 'invoice-e2e',
        creditCardId: creditCard.id,
        referenceMonth: 9,
        referenceYear: 2026,
        closingDate: '2026-09-10',
        dueDate: '2026-09-17',
        totalAmount: 100,
        status: invoiceStatus,
        paidAt: invoiceStatus === 'PAID' ? '2026-09-17' : null,
        version: invoiceStatus === 'OPEN' ? 1 : 2,
        transactions: [],
        creditAppliedAmount: 0,
      });
      return;
    }

    if (method === 'POST' && path === '/api/v1/invoices/invoice-e2e/close') {
      invoiceStatus = 'CLOSED';
      await fulfillJson(route, { id: 'invoice-e2e', status: invoiceStatus, version: 2 });
      return;
    }

    if (method === 'POST' && path === '/api/v1/invoices/invoice-e2e/pay') {
      invoiceStatus = 'PAID';
      await fulfillJson(route, {
        invoiceId: 'invoice-e2e',
        totalAmount: 100,
        creditAppliedAmount: 0,
        cashPaidAmount: 100,
        paymentTransactionId: 'payment-e2e',
        paidAt: '2026-09-17',
      });
      return;
    }

    if (path === '/api/v1/budgets' && method === 'GET') {
      await fulfillJson(route, budgets);
      return;
    }

    if (path === '/api/v1/budgets' && method === 'POST') {
      const body = request.postDataJSON() as JsonObject;
      const createdBudget = {
        id: 'budget-e2e',
        ...body,
        spentAmount: 0,
        usagePercentage: 0,
        alertStatus: 'NORMAL',
        createdAt: NOW,
        updatedAt: NOW,
      };

      budgets.push(createdBudget);
      await fulfillJson(route, createdBudget, 201);
      return;
    }

    if (method === 'GET' && path === '/api/v1/exports/transactions.csv') {
      await route.fulfill({
        status: 200,
        contentType: 'text/csv; charset=utf-8',
        headers: {
          'content-disposition': 'attachment; filename="transactions.csv"',
        },
        body: 'id,description,amount\ntransaction-e2e,Compra,1234.56\n',
      });
      return;
    }

    await fulfillJson(route, { message: `Endpoint não simulado: ${method} ${path}` }, 404);
  });
}

async function openAuthenticatedPage(page: Page, path: string): Promise<void> {
  await addAuthenticatedSession(page);
  await installFinanceApiMock(page);
  await page.goto(path);
}

test('cadastro e login levam a uma sessão autenticada', async ({ page }) => {
  await installFinanceApiMock(page);
  await page.goto('/register');

  await page.getByLabel('Nome').fill('Saul Goodman');
  await page.getByLabel('E-mail').fill('saul@example.test');
  await page.locator('#register-password').fill('Segredo123');
  await page.getByLabel('Confirmar senha').fill('Segredo123');

  const registerRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/auth/register'),
  );
  await page.getByRole('button', { name: 'Criar conta' }).click();

  expect((await registerRequest).postDataJSON()).toMatchObject({
    name: 'Saul Goodman',
    email: 'saul@example.test',
  });
  await expect(page).toHaveURL(/\/login$/);

  await page.getByLabel('E-mail').fill('saul@example.test');
  await page.locator('#login-password').fill('Segredo123');

  const loginRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/auth/login'),
  );
  await page.getByRole('button', { name: 'Entrar' }).click();

  expect((await loginRequest).postDataJSON()).toEqual({
    email: 'saul@example.test',
    password: 'Segredo123',
  });
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Dashboard financeiro' })).toBeVisible();
});

test('cria uma conta e uma categoria de despesa', async ({ page }) => {
  await openAuthenticatedPage(page, '/accounts/new');

  await page.getByLabel('Nome da conta').fill('Conta Skyler');
  await page.getByLabel('Tipo de conta').selectOption('CHECKING');
  await page.getByLabel('Instituição financeira').fill('Banco Branco');
  await page.getByLabel('Saldo inicial').fill('250.75');

  const accountRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/accounts'),
  );
  await page.getByRole('button', { name: 'Criar conta' }).click();

  expect((await accountRequest).postDataJSON()).toMatchObject({
    name: 'Conta Skyler',
    initialBalance: 250.75,
  });
  await expect(page).toHaveURL(/\/accounts\/account-e2e$/);

  await page.goto('/categories');
  await page.getByRole('button', { name: 'Nova categoria' }).first().click();
  await page.getByLabel('Nome').fill('Combustível');
  await page.locator('#category-type').selectOption('EXPENSE');

  const categoryRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/categories'),
  );
  await page.getByRole('button', { name: 'Salvar categoria' }).click();

  expect((await categoryRequest).postDataJSON()).toMatchObject({
    name: 'Combustível',
    type: 'EXPENSE',
  });
  await expect(page.getByText('Combustível')).toBeVisible();
});

test('cria receita e despesa com contas e categorias compatíveis', async ({ page }) => {
  await openAuthenticatedPage(page, '/transactions/new');

  await page.getByLabel('Tipo').selectOption('INCOME');
  await page.getByLabel('Descrição').fill('Pagamento de Walter');
  await page.getByLabel('Valor').fill('1500');
  await page.getByLabel('Data de competência').fill('2026-09-17');
  await page.getByLabel('Data efetiva').fill('2026-09-17');
  await page.getByLabel('Categoria').selectOption(incomeCategory.id);
  await page.getByLabel('Conta de destino').selectOption(primaryAccount.id);

  const incomeRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/transactions'),
  );
  await page.getByRole('button', { name: 'Criar transação' }).click();

  expect((await incomeRequest).postDataJSON()).toMatchObject({
    type: 'INCOME',
    destinationAccountId: primaryAccount.id,
    sourceAccountId: null,
  });
  await expect(page).toHaveURL(/\/transactions\/transaction-e2e$/);

  await page.goto('/transactions/new');
  await page.getByLabel('Tipo').selectOption('EXPENSE');
  await page.getByLabel('Descrição').fill('Mercado do Jesse');
  await page.getByLabel('Valor').fill('120.5');
  await page.getByLabel('Data de competência').fill('2026-09-17');
  await page.getByLabel('Data efetiva').fill('2026-09-17');
  await page.getByLabel('Categoria').selectOption(expenseCategory.id);
  await page.getByLabel('Conta de origem').selectOption(primaryAccount.id);

  const expenseRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/transactions'),
  );
  await page.getByRole('button', { name: 'Criar transação' }).click();

  expect((await expenseRequest).postDataJSON()).toMatchObject({
    type: 'EXPENSE',
    sourceAccountId: primaryAccount.id,
    destinationAccountId: null,
  });
  await expect(page).toHaveURL(/\/transactions\/transaction-e2e$/);
});

test('confirma transferência antes de enviar e bloqueia origens e destinos iguais', async ({
  page,
}) => {
  await openAuthenticatedPage(page, '/transfers/new');

  await page.getByLabel('Conta de origem').selectOption(primaryAccount.id);
  await expect(page.getByLabel('Conta de destino')).not.toHaveValue(primaryAccount.id);
  await page.getByLabel('Conta de destino').selectOption(secondaryAccount.id);
  await page.getByLabel('Valor').fill('75');
  await page.getByLabel('Data').fill('2026-09-17');
  await page.getByLabel('Descrição').fill('Transferência para Jesse');
  await page.getByRole('button', { name: 'Revisar transferência' }).click();

  const dialog = page.getByRole('dialog', { name: 'Confirmar transferência?' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toContainText('Conta Walter');
  await expect(dialog).toContainText('Conta Jesse');
  await expect(dialog.getByRole('button', { name: 'Revisar dados' })).toBeFocused();

  const transferRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/transfers'),
  );
  await dialog.getByRole('button', { name: 'Confirmar transferência' }).click();

  expect((await transferRequest).postDataJSON()).toMatchObject({
    sourceAccountId: primaryAccount.id,
    destinationAccountId: secondaryAccount.id,
    amount: 75,
  });
  await expect(page).toHaveURL(/\/transactions\/transfer-e2e$/);
});

test('registra compra no cartão e apresenta o impacto de limite sem saldo bancário', async ({
  page,
}) => {
  await openAuthenticatedPage(page, `/credit-cards/${creditCard.id}/purchases/new`);

  await expect(page.getByText('Impacto da compra')).toBeVisible();
  await page.getByLabel('Descrição').fill('Equipamento para Jesse');
  await page.getByLabel('Valor total').fill('360');
  await page.getByLabel('Data da compra').fill('2026-09-17');
  await page.getByLabel('Categoria de despesa').selectOption(expenseCategory.id);
  await page.getByLabel('Quantidade de parcelas').fill('3');

  const purchaseRequest = page.waitForRequest(
    (request) =>
      request.method() === 'POST' &&
      request.url().endsWith(`/api/v1/credit-cards/${creditCard.id}/purchases`),
  );
  await page.getByRole('button', { name: 'Registrar compra' }).click();

  expect((await purchaseRequest).postDataJSON()).toEqual({
    description: 'Equipamento para Jesse',
    amount: 360,
    purchaseDate: '2026-09-17',
    categoryId: expenseCategory.id,
    installmentCount: 3,
  });
  await expect(page).toHaveURL(new RegExp(`/credit-cards/${creditCard.id}$`));
});

test('fecha e paga a fatura apenas após confirmação explícita', async ({ page }) => {
  await openAuthenticatedPage(page, '/invoices/invoice-e2e');

  await page.getByRole('button', { name: 'Fechar fatura' }).click();
  const closeDialog = page.getByRole('dialog', { name: 'Fechar fatura?' });
  await expect(closeDialog).toBeVisible();
  const closeRequest = page.waitForRequest(
    (request) =>
      request.method() === 'POST' && request.url().endsWith('/api/v1/invoices/invoice-e2e/close'),
  );
  await closeDialog.getByRole('button', { name: 'Fechar fatura' }).click();
  expect((await closeRequest).postDataJSON()).toEqual({ expectedVersion: 1 });
  await expect(page.getByRole('button', { name: 'Pagar fatura' })).toBeVisible();

  await page.getByRole('button', { name: 'Pagar fatura' }).click();
  await page.getByRole('button', { name: 'Confirmar pagamento' }).click();
  const paymentDialog = page.getByRole('alertdialog', { name: 'Pagar fatura?' });
  await expect(paymentDialog).toBeVisible();

  const paymentRequest = page.waitForRequest(
    (request) =>
      request.method() === 'POST' && request.url().endsWith('/api/v1/invoices/invoice-e2e/pay'),
  );
  await paymentDialog.getByRole('button', { name: 'Pagar fatura' }).click();

  expect((await paymentRequest).postDataJSON()).toEqual({
    sourceAccountId: primaryAccount.id,
    expectedVersion: 2,
  });
  await expect(page.getByText('Fatura quitada')).toBeVisible();
});

test('cria orçamento e exporta as transações filtradas em CSV', async ({ page }) => {
  await openAuthenticatedPage(page, '/budgets');

  await page.getByRole('button', { name: 'Novo orçamento' }).click();
  await page.getByLabel('Categoria de despesa').selectOption(expenseCategory.id);
  await page.getByLabel('Limite mensal').fill('800');

  const budgetRequest = page.waitForRequest(
    (request) => request.method() === 'POST' && request.url().endsWith('/api/v1/budgets'),
  );
  await page
    .getByRole('region', { name: 'Novo orçamento' })
    .getByRole('button', { name: 'Criar orçamento' })
    .click();

  expect((await budgetRequest).postDataJSON()).toMatchObject({
    categoryId: expenseCategory.id,
    amountLimit: 800,
  });
  await expect(page.getByText('Consumo normal', { exact: true })).toBeVisible();

  await page.goto('/transactions');
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Exportar CSV' }).click();
  const download = await downloadPromise;

  expect(download.suggestedFilename()).toBe('transactions.csv');
});

test('preserva navegação por teclado e conteúdo em larguras desktop, tablet e mobile', async ({
  page,
}) => {
  await openAuthenticatedPage(page, '/dashboard');

  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 768, height: 1024 },
    { width: 375, height: 667 },
  ]) {
    await page.setViewportSize(viewport);
    await expect(page.getByRole('heading', { name: 'Dashboard financeiro' })).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      .toBe(true);
  }

  const skipLink = page.getByRole('link', { name: 'Pular para o conteúdo' });
  await skipLink.focus();
  await page.keyboard.press('Enter');
  await expect(page.locator('#main-content')).toBeFocused();

  await page.getByRole('button', { name: 'Abrir ou fechar menu' }).click();
  await expect(page.getByRole('navigation', { name: 'Navegação principal' })).toBeVisible();
});
