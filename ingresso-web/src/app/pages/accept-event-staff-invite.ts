import { Component, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { EntryApi } from '../core/entry-api';

@Component({
  imports: [RouterLink],
  template: `
    <section class="auth-page">
      <div class="auth-panel">
        <span class="section-kicker">EQUIPE DO EVENTO</span>
        <h1>Aceitar convite<span class="accent">.</span></h1>
        <p>Entre na conta vinculada ao convite antes de aceitá-lo.</p>
        <label for="event-staff-token">Token do convite</label>
        <input #token id="event-staff-token" autocomplete="off" spellcheck="false" />
        <button class="button-primary" type="button" [disabled]="working()" (click)="accept(token.value)">
          {{ working() ? 'Aceitando...' : 'Aceitar convite' }}
        </button>
        @if (message()) {
          <p [class]="success() ? 'notice success' : 'notice error'" role="status">{{ message() }}</p>
        }
        <a class="text-link" routerLink="/eventos">Voltar aos eventos</a>
      </div>
    </section>
  `,
})
export class AcceptEventStaffInvite {
  @ViewChild('token') private tokenInput?: ElementRef<HTMLInputElement>;

  private readonly api = inject(EntryApi);
  protected readonly working = signal(false);
  protected readonly success = signal(false);
  protected readonly message = signal('');

  protected accept(value: string): void {
    const token = value.trim();
    if (!token) {
      this.message.set('Informe o token do convite.');
      this.success.set(false);
      return;
    }
    this.working.set(true);
    this.api.acceptStaffInvite(token).subscribe({
      next: () => {
        this.success.set(true);
        this.message.set('Convite aceito. Acesso aos eventos atribuídos ativado.');
        if (this.tokenInput) this.tokenInput.nativeElement.value = '';
        this.working.set(false);
      },
      error: (error: unknown) => {
        this.success.set(false);
        this.message.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Entre com o e-mail indicado no convite.'
          : 'Convite inválido, expirado ou já utilizado.');
        this.working.set(false);
      },
    });
  }
}