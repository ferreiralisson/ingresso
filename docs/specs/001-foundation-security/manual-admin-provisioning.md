# Provisionamento Manual do Primeiro ADMIN

Este procedimento existe porque a API não oferece operação HTTP para atribuir `ADMIN`. Não use cadastro público nem altere o perfil via payload: a API ignora o campo `perfil` e cria apenas `USER`.

## Preparação

1. Configure `TOKEN_SECRET` com pelo menos 64 bytes UTF-8 e inicie a API para que Flyway crie/atualize o banco H2 em arquivo.
2. Gere localmente um hash BCrypt da senha usando `BCryptPasswordEncoder`; nunca use senha em texto claro no SQL ou em arquivo versionado.
3. Para uma instalação local controlada, pare a API e reabra-a com `H2_CONSOLE_ENABLED=true`. Não habilite nem exponha o console em ambiente acessível pela rede ou produção.
4. Abra `http://localhost:8081/h2-console` e use o mesmo JDBC URL/usuário configurados em `INGRESSO_DB_URL` e `INGRESSO_DB_USERNAME` (padrão `jdbc:h2:file:./data/ingresso`, usuário `sa`).

## SQL

Substitua os valores entre `<...>` e execute como uma única operação. O hash deve ser BCrypt e o e-mail deve ser exclusivo.

```sql
INSERT INTO usuarios (nome, email, senha)
VALUES ('Administrador inicial', 'admin@exemplo.com', '<bcrypt-hash>');

INSERT INTO usuario_perfis (usuario_id, perfil)
SELECT id, 'USER' FROM usuarios WHERE email = 'admin@exemplo.com';

INSERT INTO usuario_perfis (usuario_id, perfil)
SELECT id, 'ADMIN' FROM usuarios WHERE email = 'admin@exemplo.com';

INSERT INTO permission_audits (
    actor_user_id, actor_email, target_user_id, target_email, permission, action, occurred_at
)
SELECT NULL, 'manual-database-provisioning', id, email, 'ADMIN',
       'INITIAL_ADMIN_PROVISIONED', CURRENT_TIMESTAMP
FROM usuarios WHERE email = 'admin@exemplo.com';
```

Confirme que a conta possui `USER` e `ADMIN` na tabela `usuario_perfis` e que a ação inicial aparece em `permission_audits`. Depois, desabilite `H2_CONSOLE_ENABLED` e reinicie a API. Não remova nem rebaixe o último administrador sem primeiro provisionar e validar um substituto.

## Migração de contas existentes

A migração preserva contas e credenciais existentes. Como a versão anterior permitia solicitar `ADMIN` no cadastro público, todos os grants legados `ADMIN` são convertidos para `USER` e registrados como `LEGACY_ADMIN_DEMOTED`. Reprovisione somente administradores confiáveis após revisar esses registros.