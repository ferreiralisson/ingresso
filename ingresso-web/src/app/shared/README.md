# Componentes compartilhados

`AuthLayout` é o único componente compartilhado encontrado. Fornece composição visual comum às telas de login e cadastro por meio de `ng-content`.

Depende somente de Angular e estilos globais definidos em `src/styles.css`/`src/app/app.css`; é consumido pelas páginas de autenticação. Não possui estado de usuário, lógica HTTP ou regras de negócio. A ilustração visual de ingresso não corresponde a uma funcionalidade de emissão ou compra.