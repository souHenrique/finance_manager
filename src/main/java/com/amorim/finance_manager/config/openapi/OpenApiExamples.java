package com.amorim.finance_manager.config.openapi;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String REGISTER_REQUEST = """
            {
              "name": "Henrique Amorim",
              "email": "henrique@example.com",
              "password": "SenhaSegura123"
            }
            """;

    public static final String USER_RESPONSE = """
            {
              "id": "2a1fbc5b-cbb9-4879-b0c5-42f034d64261",
              "name": "Henrique Amorim",
              "email": "henrique@example.com",
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:00:00Z"
            }
            """;

    public static final String UPDATED_USER_RESPONSE = """
            {
              "id": "2a1fbc5b-cbb9-4879-b0c5-42f034d64261",
              "name": "Henrique Amorim Silva",
              "email": "henrique.silva@example.com",
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String LOGIN_REQUEST = """
            {
              "email": "henrique@example.com",
              "password": "SenhaSegura123"
            }
            """;

    public static final String AUTH_RESPONSE = """
            {
              "token": "eyJhbGciOiJIUzI1NiJ9...",
              "tokenType": "Bearer",
              "expiresIn": 3600
            }
            """;

    public static final String CREATE_ACCOUNT_REQUEST = """
            {
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00
            }
            """;

    public static final String ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "ACTIVE",
              "version": 0,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:00:00Z"
            }
            """;

    public static final String ACCOUNT_LIST_RESPONSE = """
            [
              {
                "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
                "name": "Conta principal",
                "type": "CHECKING",
                "institution": "Banco Exemplo",
                "initialBalance": 1500.00,
                "currentBalance": 1500.00,
                "status": "ACTIVE",
                "version": 0,
                "createdAt": "2026-09-02T12:00:00Z",
                "updatedAt": "2026-09-02T12:00:00Z"
              },
              {
                "id": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
                "name": "Reserva de emergência",
                "type": "SAVINGS",
                "institution": "Banco Exemplo",
                "initialBalance": 500.00,
                "currentBalance": 750.00,
                "status": "ACTIVE",
                "version": 1,
                "createdAt": "2026-09-02T12:00:00Z",
                "updatedAt": "2026-09-02T12:30:00Z"
              }
            ]
            """;

    public static final String UPDATED_ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal atualizada",
              "type": "CHECKING",
              "institution": "Novo Banco",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "ACTIVE",
              "version": 1,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String INACTIVE_ACCOUNT_RESPONSE = """
            {
              "id": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "name": "Conta principal",
              "type": "CHECKING",
              "institution": "Banco Exemplo",
              "initialBalance": 1500.00,
              "currentBalance": 1500.00,
              "status": "INACTIVE",
              "version": 1,
              "createdAt": "2026-09-02T12:00:00Z",
              "updatedAt": "2026-09-02T12:30:00Z"
            }
            """;

    public static final String VALIDATION_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 400,
              "code": "VALIDATION_ERROR",
              "message": "Dados de entrada inválidos",
              "path": "/api/v1/accounts",
              "fieldErrors": [
                {
                  "field": "name",
                  "message": "Nome é obrigatório"
                }
              ]
            }
            """;

    public static final String UNAUTHORIZED_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 401,
              "code": "UNAUTHORIZED",
              "message": "Autenticação necessária ou token inválido",
              "path": "/api/v1/accounts",
              "fieldErrors": []
            }
            """;

    public static final String ACCOUNT_NOT_FOUND = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 404,
              "code": "ACCOUNT_NOT_FOUND",
              "message": "Conta não encontrada",
              "path": "/api/v1/accounts/0f6d7313-77f8-4b48-a63d-5338dd95461e",
              "fieldErrors": []
            }
            """;

    public static final String INTERNAL_SERVER_ERROR = """
            {
              "timestamp": "2026-09-02T12:00:00Z",
              "status": 500,
              "code": "INTERNAL_SERVER_ERROR",
              "message": "Ocorreu um erro interno inesperado",
              "path": "/api/v1/accounts",
              "fieldErrors": []
            }
            """;

    public static final String UPDATE_PROFILE_REQUEST = """
        {
          "name": "Henrique Amorim Silva",
          "email": "henrique.silva@example.com"
        }
        """;

    public static final String UPDATE_ACCOUNT_REQUEST = """
        {
          "name": "Conta principal atualizada",
          "type": "CHECKING",
          "institution": "Novo Banco"
        }
        """;

    public static final String UPDATE_ACCOUNT_STATUS_REQUEST = """
        {
          "status": "INACTIVE"
        }
        """;

    public static final String PARTIAL_ACCOUNT_UPDATE_REQUEST = """
        {
          "name": "Reserva de emergência"
        }
        """;

    public static final String CREATE_CATEGORY_REQUEST = """
        {
          "name": "Alimentação",
          "type": "EXPENSE",
          "parentCategoryId": null
        }
        """;

    public static final String CREATE_SUBCATEGORY_REQUEST = """
        {
          "name": "Supermercado",
          "type": "EXPENSE",
          "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928"
        }
        """;

    public static final String UPDATE_CATEGORY_REQUEST = """
        {
          "name": "Alimentação e mercado",
          "status": "ACTIVE"
        }
        """;

    public static final String CATEGORY_RESPONSE = """
        {
          "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "name": "Alimentação",
          "type": "EXPENSE",
          "parentCategoryId": null,
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String SUBCATEGORY_RESPONSE = """
        {
          "id": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "name": "Supermercado",
          "type": "EXPENSE",
          "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String UPDATED_CATEGORY_RESPONSE = """
        {
          "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "name": "Alimentação e mercado",
          "type": "EXPENSE",
          "parentCategoryId": null,
          "status": "ACTIVE",
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CATEGORY_LIST_RESPONSE = """
        [
          {
            "id": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
            "name": "Alimentação",
            "type": "EXPENSE",
            "parentCategoryId": null,
            "status": "ACTIVE",
            "createdAt": "2026-09-02T12:00:00Z",
            "updatedAt": "2026-09-02T12:00:00Z"
          },
          {
            "id": "57b1879c-a98e-4718-b66d-47f970ab6709",
            "name": "Supermercado",
            "type": "EXPENSE",
            "parentCategoryId": "c487c4cf-d948-4ba8-a85f-e36bb798c928",
            "status": "ACTIVE",
            "createdAt": "2026-09-02T12:00:00Z",
            "updatedAt": "2026-09-02T12:00:00Z"
          }
        ]
        """;

    public static final String CREATE_EXPENSE_TRANSACTION_REQUEST = """
        {
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String CREATE_INCOME_TRANSACTION_REQUEST = """
        {
          "description": "Salário mensal",
          "amount": 5000.00,
          "competenceDate": "2026-09-01",
          "effectiveDate": "2026-09-01",
          "dueDate": null,
          "type": "INCOME",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": null,
          "destinationAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String CREATE_PENDING_TRANSACTION_REQUEST = """
        {
          "description": "Conta de energia",
          "amount": 240.75,
          "competenceDate": "2026-09-02",
          "effectiveDate": null,
          "dueDate": "2026-09-10",
          "type": "EXPENSE",
          "status": "PENDING",
          "paymentMethod": "OTHER",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "ae68eab0-5738-42b3-9898-07191e036e5b",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null
        }
        """;

    public static final String UPDATE_TRANSACTION_REQUEST = """
        {
          "description": "Compra mensal no supermercado",
          "amount": 210.90,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "status": "COMPLETED",
          "paymentMethod": "DEBIT",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709"
        }
        """;

    public static final String UPDATE_TRANSACTION_AMOUNT_REQUEST = """
        {
          "amount": 210.90
        }
        """;

    public static final String TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String UPDATED_TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra mensal no supermercado",
          "amount": 210.90,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "COMPLETED",
          "paymentMethod": "DEBIT",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CANCELLED_TRANSACTION_RESPONSE = """
        {
          "id": "2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "description": "Compra no supermercado",
          "amount": 180.50,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": "2026-09-02",
          "type": "EXPENSE",
          "status": "CANCELLED",
          "paymentMethod": "PIX",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": null,
          "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:30:00Z"
        }
        """;

    public static final String CREATE_TRANSFER_REQUEST = """
        {
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
          "amount": 250.00,
          "date": "2026-09-02",
          "description": "Transferência para reserva"
        }
        """;

    public static final String TRANSFER_RESPONSE = """
        {
          "id": "3707f594-1649-4670-959c-1702e03af86d",
          "description": "Transferência para reserva",
          "amount": 250.00,
          "competenceDate": "2026-09-02",
          "effectiveDate": "2026-09-02",
          "dueDate": null,
          "type": "TRANSFER",
          "status": "COMPLETED",
          "paymentMethod": "TRANSFER",
          "sourceAccountId": "0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "destinationAccountId": "9ba25043-024c-4ba3-a48a-bf62a2c30ef0",
          "categoryId": null,
          "creditCardId": null,
          "invoiceId": null,
          "installmentGroupId": null,
          "installmentNumber": null,
          "installmentCount": null,
          "createdAt": "2026-09-02T12:00:00Z",
          "updatedAt": "2026-09-02T12:00:00Z"
        }
        """;

    public static final String CATEGORY_NOT_FOUND = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 404,
          "code": "CATEGORY_NOT_FOUND",
          "message": "Categoria não encontrada",
          "path": "/api/v1/categories/c487c4cf-d948-4ba8-a85f-e36bb798c928",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_NOT_FOUND = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 404,
          "code": "TRANSACTION_NOT_FOUND",
          "message": "Transação não encontrada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "fieldErrors": []
        }
        """;

    public static final String EMAIL_ALREADY_EXISTS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "EMAIL_ALREADY_EXISTS",
          "message": "E-mail já cadastrado",
          "path": "/api/v1/auth/register",
          "fieldErrors": []
        }
        """;

    public static final String PROFILE_EMAIL_ALREADY_EXISTS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "EMAIL_ALREADY_EXISTS",
          "message": "E-mail já cadastrado",
          "path": "/api/v1/users/me",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_TRANSACTION_STATUS = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "INVALID_TRANSACTION_STATUS",
          "message": "Transação cancelada não pode ser editada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231",
          "fieldErrors": []
        }
        """;

    public static final String OPTIMISTIC_LOCK_CONFLICT = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "OPTIMISTIC_LOCK_CONFLICT",
          "message": "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.",
          "path": "/api/v1/accounts/0f6d7313-77f8-4b48-a63d-5338dd95461e",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_ALREADY_CANCELLED = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 409,
          "code": "TRANSACTION_ALREADY_CANCELLED",
          "message": "Transação já está cancelada",
          "path": "/api/v1/transactions/2cb0ba91-bfc4-43be-89ec-336ca64a6231/cancel",
          "fieldErrors": []
        }
        """;

    public static final String INVALID_CREDENTIALS_ERROR = """
        {
          "timestamp": "2026-09-02T12:00:00Z",
          "status": 401,
          "code": "INVALID_CREDENTIALS",
          "message": "Credenciais inválidas",
          "path": "/api/v1/auth/login",
          "fieldErrors": []
        }
        """;

    public static final String TRANSACTION_PAGE_RESPONSE = """
        {
          "content": [],
          "page": 0,
          "size": 20,
          "totalElements": 0,
          "totalPages": 0,
          "first": true,
          "last": true
        }
        """;

    public static final String DAILY_CASH_FLOW_RESPONSE = """
        {
          "date": "2026-09-03",
          "summary": {
            "inflows": 5000.00,
            "outflows": 1500.00,
            "net": 3500.00,
            "invoicePayments": 1200.00,
            "incomeCategories": [
              {
                "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                "name": "Salário",
                "amount": 5000.00
              }
            ],
            "expenseCategories": [
              {
                "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                "name": "Supermercado",
                "amount": 300.00
              }
            ]
          }
        }
        """;

    public static final String WEEKLY_CASH_FLOW_RESPONSE = """
        {
          "currentWeek": {
            "startDate": "2026-08-31",
            "endDate": "2026-09-06",
            "summary": {
              "inflows": 5000.00,
              "outflows": 2000.00,
              "net": 3000.00,
              "invoicePayments": 1200.00,
              "incomeCategories": [
                {
                  "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                  "name": "Salário",
                  "amount": 5000.00
                }
              ],
              "expenseCategories": [
                {
                  "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                  "name": "Supermercado",
                  "amount": 800.00
                }
              ]
            }
          },
          "previousWeek": {
            "startDate": "2026-08-24",
            "endDate": "2026-08-30",
            "summary": {
              "inflows": 4500.00,
              "outflows": 1800.00,
              "net": 2700.00,
              "invoicePayments": 1000.00,
              "incomeCategories": [
                {
                  "categoryId": "14e7b32a-e52f-4e04-9812-4c8067129684",
                  "name": "Salário",
                  "amount": 4500.00
                }
              ],
              "expenseCategories": [
                {
                  "categoryId": "57b1879c-a98e-4718-b66d-47f970ab6709",
                  "name": "Supermercado",
                  "amount": 800.00
                }
              ]
            }
          },
          "comparison": {
            "inflowsDifference": 500.00,
            "outflowsDifference": 200.00,
            "netDifference": 300.00
          }
        }
        """;

    public static final String DAILY_CASH_DATE_REQUIRED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "VALIDATION_ERROR",
          "message": "Dados de entrada inválidos",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": [
            {
              "field": "date",
              "message": "A data é obrigatória"
            }
          ]
        }
        """;

    public static final String WEEKLY_CASH_DATE_REQUIRED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "VALIDATION_ERROR",
          "message": "Dados de entrada inválidos",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": [
            {
              "field": "date",
              "message": "A data é obrigatória"
            }
          ]
        }
        """;

    public static final String DAILY_CASH_INVALID_PERIOD_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "INVALID_REPORT_PERIOD",
          "message": "O período deve estar entre 0001-01-01 e 9999-12-31",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_INVALID_PERIOD_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 400,
          "code": "INVALID_REPORT_PERIOD",
          "message": "O período deve estar entre 0001-01-01 e 9999-12-31",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;

    public static final String DAILY_CASH_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária ou token inválido",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_UNAUTHORIZED_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 401,
          "code": "UNAUTHORIZED",
          "message": "Autenticação necessária ou token inválido",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;

    public static final String DAILY_CASH_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Ocorreu um erro interno inesperado",
          "path": "/api/v1/reports/cash/daily",
          "fieldErrors": []
        }
        """;

    public static final String WEEKLY_CASH_INTERNAL_ERROR = """
        {
          "timestamp": "2026-09-03T12:00:00Z",
          "status": 500,
          "code": "INTERNAL_SERVER_ERROR",
          "message": "Ocorreu um erro interno inesperado",
          "path": "/api/v1/reports/cash/weekly",
          "fieldErrors": []
        }
        """;
}
