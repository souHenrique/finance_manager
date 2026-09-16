# Finance Manager Frontend

Frontend do Finance Manager desenvolvido com Angular 22, TypeScript, SCSS e arquitetura baseada em features.

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
pnpm lint
pnpm lint:fix
pnpm format
pnpm format:check
```

## Configuração da API

A URL base da API está definida nos arquivos de src/environments.

Não coloque senhas, tokens ou outros segredos nesses arquivos, pois os valores do frontend são incorporados ao bundle e ficam visíveis no navegador.
