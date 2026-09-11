# Gestão financeira

API REST para gestão financeira pessoal, com controle de contas, categorias,
transações, transferências, cartões de crédito, faturas, orçamentos, relatórios,
dashboard e exportação de transações em CSV.

O projeto aplica isolamento dos dados por usuário, autenticação JWT, validações
de negócio, controle transacional, concorrência otimista, auditoria de entidades
financeiras e migrações versionadas do banco de dados.

## Funcionalidades implementadas

- cadastro, autenticação e atualização do perfil do usuário;
- contas financeiras com saldo atual e ativação/inativação;
- categorias de receita e despesa, incluindo relacionamento hierárquico;
- receitas, despesas, PIX e transferências entre contas;
- pesquisa paginada de transações com filtros combináveis;
- cartões de crédito e compras à vista ou parceladas;
- geração, consulta, fechamento e pagamento de faturas;
- estorno de compras no cartão e aplicação de créditos em faturas;
- orçamentos mensais por categoria, com cálculo de consumo e alertas;
- relatórios de caixa diário, semanal, mensal e anual;
- relatório financeiro por competência;
- cálculo de patrimônio líquido;
- dashboard financeiro com identificação explícita do regime de cada indicador;
- exportação das transações filtradas em CSV;
- documentação OpenAPI e Swagger UI;
- auditoria de contas, transações, cartões e faturas com Hibernate Envers.

## Tecnologias

- Java 25;
- Spring Boot 4.1;
- Spring Web MVC;
- Spring Data JPA e Hibernate;
- Spring Security e JWT;
- PostgreSQL 16;
- Flyway;
- Hibernate Envers;
- MapStruct e Lombok;
- Springdoc OpenAPI;
- JUnit 5, Mockito e Testcontainers;
- Maven Wrapper e Docker Compose.

## Requisitos

- JDK 25;
- Docker com Docker Compose;
- nenhuma instalação global do Maven é necessária, pois o projeto inclui o
  Maven Wrapper.

## Configuração

Use `.env.example` como referência:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=change-me-locally
DB_USERNAME=change-me-locally
DB_PASSWORD=change-me-locally
JWT_SECRET=replace-with-a-base64-secret-of-at-least-32-bytes
JWT_EXPIRATION=3600000
```

`JWT_EXPIRATION` é informado em milissegundos. Não utilize o segredo de exemplo
fora do ambiente local.

O Docker Compose lê o arquivo `.env`, mas a aplicação também precisa receber
essas variáveis no ambiente do processo. Configure-as na IDE ou carregue-as no
terminal antes de iniciar a aplicação.

### PowerShell

```powershell
Copy-Item .env.example .env

Get-Content .env |
    Where-Object { $_ -match '^[^#].+=' } |
    ForEach-Object {
        $name, $value = $_ -split '=', 2
        Set-Item -Path "Env:$name" -Value $value
    }

.\mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
cp .env.example .env
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

No perfil padrão `dev`, a integração do Spring Boot com Docker Compose inicia o
PostgreSQL definido em `docker-compose.yml`. A API fica disponível em
`http://localhost:8080`.

Para produção, use o perfil `prod` e configure `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET` e, opcionalmente, `JWT_EXPIRATION`.

## Autenticação

Somente cadastro, login e documentação da API são públicos. Os demais endpoints
exigem um token JWT no cabeçalho:

```http
Authorization: Bearer <token>
```

Exemplo de cadastro:

```bash
curl --request POST http://localhost:8080/api/v1/auth/register \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Henrique Amorim",
    "email": "henrique@example.com",
    "password": "SenhaSegura123"
  }'
```

Exemplo de login:

```bash
curl --request POST http://localhost:8080/api/v1/auth/login \
  --header "Content-Type: application/json" \
  --data '{
    "email": "henrique@example.com",
    "password": "SenhaSegura123"
  }'
```

## Endpoints

### Usuário, contas e categorias

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Cadastrar usuário |
| `POST` | `/api/v1/auth/login` | Autenticar e obter JWT |
| `GET` | `/api/v1/users/me` | Consultar o perfil autenticado |
| `PATCH` | `/api/v1/users/me` | Atualizar o perfil autenticado |
| `POST` | `/api/v1/accounts` | Criar conta |
| `GET` | `/api/v1/accounts` | Listar contas |
| `GET` | `/api/v1/accounts/{id}` | Consultar conta |
| `PATCH` | `/api/v1/accounts/{id}` | Atualizar conta |
| `PATCH` | `/api/v1/accounts/{id}/status` | Alterar o status da conta |
| `POST` | `/api/v1/categories` | Criar categoria |
| `GET` | `/api/v1/categories` | Listar categorias |
| `GET` | `/api/v1/categories/{id}` | Consultar categoria |
| `PATCH` | `/api/v1/categories/{id}` | Atualizar categoria |

### Transações e transferências

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/transactions` | Criar receita ou despesa |
| `GET` | `/api/v1/transactions` | Pesquisar transações |
| `GET` | `/api/v1/transactions/{id}` | Consultar transação |
| `PATCH` | `/api/v1/transactions/{id}` | Atualizar transação |
| `POST` | `/api/v1/transactions/{id}/cancel` | Cancelar transação |
| `POST` | `/api/v1/transfers` | Transferir valores entre contas |

### Cartões e faturas

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/credit-cards` | Criar cartão de crédito |
| `GET` | `/api/v1/credit-cards` | Listar cartões |
| `GET` | `/api/v1/credit-cards/{id}` | Consultar cartão |
| `PATCH` | `/api/v1/credit-cards/{id}` | Atualizar cartão |
| `POST` | `/api/v1/credit-cards/{id}/purchases` | Registrar compra à vista ou parcelada |
| `POST` | `/api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund` | Estornar compra no cartão |
| `GET` | `/api/v1/invoices` | Pesquisar faturas |
| `GET` | `/api/v1/invoices/{id}` | Consultar fatura e transações |
| `GET` | `/api/v1/credit-cards/{id}/invoices` | Listar faturas de um cartão |
| `POST` | `/api/v1/invoices/{id}/close` | Fechar fatura |
| `POST` | `/api/v1/invoices/{id}/pay` | Pagar fatura |

### Orçamentos, relatórios e dashboard

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/budgets` | Criar orçamento mensal |
| `GET` | `/api/v1/budgets` | Listar orçamentos |
| `GET` | `/api/v1/budgets/{id}` | Consultar orçamento |
| `PATCH` | `/api/v1/budgets/{id}` | Atualizar orçamento |
| `DELETE` | `/api/v1/budgets/{id}` | Excluir definitivamente orçamento |
| `GET` | `/api/v1/reports/cash/daily?date=YYYY-MM-DD` | Relatório de caixa diário |
| `GET` | `/api/v1/reports/cash/weekly?date=YYYY-MM-DD` | Relatório e comparação semanal |
| `GET` | `/api/v1/reports/cash/monthly?year=YYYY&month=M` | Relatório de caixa mensal |
| `GET` | `/api/v1/reports/cash/annual?year=YYYY` | Evolução anual do caixa |
| `GET` | `/api/v1/reports/competence?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | Relatório por competência |
| `GET` | `/api/v1/dashboard` | Dashboard financeiro consolidado |
| `GET` | `/api/v1/exports/transactions.csv` | Exportar transações filtradas em CSV |


## Exportação CSV

O endpoint `GET /api/v1/exports/transactions.csv` reutiliza o mesmo objeto de
filtros, a mesma validação e a mesma especificação da pesquisa de transações.
Ele exporta todos os registros encontrados, sem os parâmetros de paginação.

Exemplo:

```bash
curl --get http://localhost:8080/api/v1/exports/transactions.csv \
  --header "Authorization: Bearer <token>" \
  --data-urlencode "startDate=2026-09-01" \
  --data-urlencode "endDate=2026-09-30" \
  --data-urlencode "status=COMPLETED" \
  --output transactions.csv
```

Contrato do arquivo:

- codificação UTF-8, sem BOM;
- mídia `text/csv;charset=UTF-8`;
- download com o nome `transactions.csv`;
- separador por vírgula e linhas terminadas por `CRLF`;
- campos com vírgula, aspas ou quebras de linha são envolvidos por aspas;
- aspas internas são duplicadas;
- datas seguem ISO-8601 (`YYYY-MM-DD`);
- datas ausentes são exportadas como campo vazio;
- valores monetários usam representação decimal simples, como `1234.56`;
- ordenação por `competenceDate` decrescente e `id` crescente;
- categorias e contas são resolvidas somente dentro do usuário autenticado;
- para transferências, `account` é representado como `Origem -> Destino`.

Cabeçalho estável:

```csv
id,description,type,status,amount,competenceDate,effectiveDate,category,account
```

Quando não há resultados, o arquivo contém somente o cabeçalho. Compras no
cartão ficam com `account` vazio porque não movimentam uma conta no momento da
compra.

## Documentação da API

Com a aplicação em execução:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Os contratos documentam schemas públicos, parâmetros, exemplos, autenticação e
respostas de erro padronizadas.

## Banco de dados e migrações

O PostgreSQL é versionado pelo Flyway. As migrações estão em
`src/main/resources/db/migration` e são aplicadas automaticamente na
inicialização.

O Hibernate utiliza `ddl-auto: validate`: a aplicação valida o schema, mas não o
modifica automaticamente. Atualmente existem migrações de `V1` a `V11`,
incluindo usuários, contas, categorias, transações, auditoria, cartões, faturas,
estornos, créditos e orçamentos.

## Testes

Os testes de integração utilizam PostgreSQL real por meio do Testcontainers.
Mantenha o Docker em execução.

### PowerShell

```powershell
.\mvnw.cmd test
```

### Linux ou macOS

```bash
./mvnw test
```

A suíte cobre regras de domínio, autenticação e isolamento por usuário,
persistência, concorrência, relatórios, documentação OpenAPI, dashboard e o
contrato completo da exportação CSV.
