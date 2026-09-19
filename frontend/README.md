# Nummo Web

Frontend do Nummo desenvolvido com Angular 22, TypeScript, SCSS e arquitetura baseada em features.

## Requisitos

- Node.js 24.15 ou superior dentro da versão 24;

- pnpm 11.19;

- backend disponível em http://localhost:8080.

## Instalação

```
pnpm install
```

## Execução

```
pnpm start
```

A aplicação ficará disponível em:

```
http://localhost:4200
```

Durante o desenvolvimento, as chamadas iniciadas por /api são encaminhadas para o backend em http://localhost:8080 por meio de proxy.config.json.

## Estrutura

```
src/app/
├── core/       Configurações e serviços globais
├── shared/     Elementos reutilizáveis
├── layout/     Estrutura visual da aplicação
├── features/   Funcionalidades organizadas por domínio
└── app.routes.ts
```

## Scripts

```bash
pnpm start
pnpm build
pnpm test
pnpm test:ci
pnpm e2e
pnpm e2e:ui
pnpm lint
pnpm lint:fix
pnpm format
pnpm format:check
```

## Configuração da API

A aplicação carrega `runtime-config.json` antes do bootstrap. Em desenvolvimento,
o arquivo público usa `/api/v1` e o proxy encaminha as chamadas para o backend
local. No build de produção, o script `scripts/write-runtime-config.mjs` gera
esse mesmo arquivo no artefato final a partir de `API_BASE_URL`.

Não coloque senhas, tokens ou outros segredos nesse arquivo, em
`src/environments` ou em qualquer variável usada pelo frontend: valores do
navegador ficam visíveis no bundle ou na rede. A configuração aceita apenas a URL
pública da API.

## Testes E2E

Os testes E2E cobrem os fluxos financeiros críticos em um navegador real, incluindo cadastro e login, contas, categorias, transações, transferências, compras no cartão, faturas, orçamentos e exportação CSV. Eles simulam as respostas da API no navegador, por isso não criam, alteram nem removem dados do ambiente de desenvolvimento.

Antes da primeira execução, instale o navegador do Playwright:

```bash
pnpm exec playwright install chromium
```

Para executar a suíte, use:

```bash
pnpm e2e
```

O comando inicia temporariamente a aplicação em `http://127.0.0.1:4200`. Para depurar os fluxos visualmente, use `pnpm e2e:ui`.
