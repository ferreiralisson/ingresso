import { Component, Input, OnInit, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { EntryApi, EventStaffGrant, EventStaffInvite } from '../core/entry-api';

@Component({
  selector: 'app-event-staff-manager',
  template: `
    @if (canManage()) {
      <section class="entry-team-manager" aria-labelledby="event-team-title">
        <div class="event-section-heading">
          <div>
            <span class="section-kicker">OPERAÇÃO</span>
            <h2 id="event-team-title">Equipe de entrada</h2>
          </div>
        </div>
        <div class="entry-team-invite">
          <label [for]="'staff-email-' + eventId">E-mail do operador</label>
          <div>
            <input #email [id]="'staff-email-' + eventId" type="email" autocomplete="email" required />
            <button type="button" class="button-secondary" [disabled]="working()" (click)="invite()">Convidar</button>
          </div>
        </div>
        @if (inviteToken(); as invite) {
          <div class="notice entry-invite-token" role="status">
            <span>Convite para {{ invite.email }} · expira {{ formatDate(invite.expiresAt) }}</span>
            <code>{{ invite.token }}</code>
            <button type="button" class="text-link" (click)="copyToken(invite.token)">Copiar token</button>
          </div>
        }
        @if (message()) {
          <p class="entry-team-message" role="status">{{ message() }}</p>
        }
        <ul class="entry-team-list">
          @for (grant of grants(); track grant.id) {
            <li>
              <span>{{ grant.email }}</span>
              <button type="button" class="text-link" [disabled]="working()" (click)="revoke(grant)">Revogar</button>
            </li>
          } @empty {
            <li class="entry-team-empty">Nenhum operador aceitou convite para este evento.</li>
          }
        </ul>
      </section>
    }
  `,
})
export class EventStaffManager implements OnInit {
  @Input({ required: true }) eventId = 0;
  @ViewChild('email') private emailInput?: ElementRef<HTMLInputElement>;

  private readonly api = inject(EntryApi);
  protected readonly canManage = signal(false);
  protected readonly grants = signal<EventStaffGrant[]>([]);
  protected readonly inviteToken = signal<EventStaffInvite | null>(null);
  protected readonly message = signal('');
  protected readonly working = signal(false);

  ngOnInit(): void {
    this.load();
  }

  protected invite(): void {
    const email = this.emailInput?.nativeElement.value.trim() ?? '';
    if (!email) {
      this.message.set('Informe um e-mail válido.');
      return;
    }
    this.working.set(true);
    this.message.set('');
    this.api.inviteStaff(this.eventId, email).subscribe({
      next: (invite) => {
        this.inviteToken.set(invite);
        this.message.set('Envie este token ao operador por um canal seguro.');
        if (this.emailInput) this.emailInput.nativeElement.value = '';
        this.working.set(false);
      },
      error: (error: unknown) => {
        this.message.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Somente o produtor dono ou ADMIN pode gerenciar a equipe.'
          : 'Não foi possível criar o convite. Verifique o e-mail e tente novamente.');
        this.working.set(false);
      },
    });
  }

  protected revoke(grant: EventStaffGrant): void {
    this.working.set(true);
    this.api.revokeStaff(this.eventId, grant.id).subscribe({
      next: () => {
        this.grants.update((current) => current.filter((item) => item.id !== grant.id));
        this.message.set(`Acesso de ${grant.email} revogado.`);
        this.working.set(false);
      },
      error: () => {
        this.message.set('Não foi possível revogar o acesso. Atualize a lista e tente novamente.');
        this.working.set(false);
      },
    });
  }

  protected async copyToken(token: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(token);
      this.message.set('Token copiado.');
    } catch {
      this.message.set('Selecione e copie o token manualmente.');
    }
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value));
  }

  private load(): void {
    this.api.listStaff(this.eventId).subscribe({
      next: (grants) => {
        this.canManage.set(true);
        this.grants.set(grants);
      },
    });
  }
}