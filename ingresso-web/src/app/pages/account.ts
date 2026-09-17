import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  imports: [DatePipe],
  template: `
    <section class="account-layout">
      <div class="account-heading">
        <span class="section-kicker">MINHA CONTA</span
        ><span class="status-badge"><span></span> Sessão iniciada</span>
      </div>
      <h1>Bom ter você aqui<span class="accent">.</span></h1>
      <p class="account-intro">Tudo começa com uma conexão. A sua já está feita.</p>
      <div class="account-grid">
        <article class="profile-card">
          <div class="avatar" aria-hidden="true">
            {{ auth.session()?.email?.charAt(0)?.toUpperCase() }}
          </div>
          <h2>Seu acesso</h2>
          <p>Você entrou com sucesso na sua conta.</p>
          <dl>
            <div>
              <dt>E-mail da conta</dt>
              <dd>{{ auth.session()?.email }}</dd>
            </div>
            <div>
              <dt>Sessão válida até</dt>
              <dd>{{ auth.session()?.expiresAt | date: 'dd/MM/yyyy, HH:mm' }}</dd>
            </div>
          </dl>
          <button class="button-secondary" (click)="logout()">
            Sair da minha conta <span aria-hidden="true">↗</span>
          </button>
          <small>Ao sair, será preciso entrar novamente.</small>
        </article>
        <article class="welcome-card">
          <span class="eyebrow">O COMEÇO DE UMA NOVA HISTÓRIA</span
          ><span class="welcome-spark" aria-hidden="true">✳</span>
          <h2>Você está<br />no lugar certo.</h2>
          <p>
            Seu acesso está pronto. Novas experiências serão disponibilizadas por aqui conforme a
            plataforma evoluir.
          </p>
          <span class="coming-soon">Mais novidades em breve <span aria-hidden="true">↗</span></span>
        </article>
      </div>
    </section>
  `,
})
export class Account {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected logout(): void {
    this.auth.clearSession();
    void this.router.navigate(['/entrar']);
  }
}
