# Ingresso

Marketplace web e API para eventos, ofertas, pedidos e operação de entrada. A API usa pagamento e reembolsos simulados; não há movimentação de dinheiro real.

## Pré-requisitos

- Java 21
- Node.js 22.22.3 ou superior na linha 22 e npm
- macOS, Linux ou Windows com shell compatível

## Executar localmente

Inicie a API no primeiro terminal, a partir da raiz do repositório:

```sh
cd ingresso-api
export TOKEN_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run
```

O segredo precisa ter pelo menos 64 bytes UTF-8. A API inicia por padrão em `http://localhost:8081`. O banco H2 persistente fica em `ingresso-api/data/ingresso`; Flyway aplica as migrations automaticamente.

Em outro terminal, também a partir da raiz:

```sh
cd ingresso-web
npm ci
npm start
```

Abra `http://localhost:4200`. O proxy de desenvolvimento encaminha `/api/**` para `http://localhost:8081`. Crie uma conta e entre para usar os fluxos autenticados.

## Console H2

O console fica desabilitado por padrão. Para habilitá-lo durante uma sessão de desenvolvimento local, mantenha `TOKEN_SECRET` configurado e inicie a API assim:

```sh
cd ingresso-api
export TOKEN_SECRET="$(openssl rand -hex 32)"
export H2_CONSOLE_ENABLED=true
./mvnw spring-boot:run
```

Abra `http://localhost:8081/h2-console/` e use o JDBC URL `jdbc:h2:file:./data/ingresso`, usuário `sa` e senha vazia, salvo se você configurou valores diferentes. Use o console somente em ambiente local confiável; não o exponha em produção.

## Testar e compilar

API:

```sh
cd ingresso-api
./mvnw test
```

Frontend:

```sh
cd ingresso-web
npm test -- --watch=false
npm run build
```

## Módulos e documentação

- [API](ingresso-api/README.md)
- [Frontend](ingresso-web/README.md)
- [Arquitetura do sistema](Arquitetura%20do%20Sistema.md)
- [Funcionalidades e sequência de implementação](Funcionalidades%20a%20Implementar.md)
- [Objetivo do sistema](Objetivo%20do%20Sistema.md)
- Especificações de features: [`specs/`](specs/)