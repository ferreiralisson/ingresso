import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { EventApi, EventStatus, EventSummary } from '../core/event-api';

@Component({
  imports: [RouterLink],
  template: `
    <section class="market-page">
      <div class="market-heading">
        <div>
          <span class="section-kicker">OPERAÇÃO</span>
          <h1>Eventos da plataforma<span class="accent">.</span></h1>
          <p class="market-intro">Suspender ou cancelar eventos publicados.</p>
        </div>
        <a class="market-action" routerLink="/admin/financeiro">Relatórios financeiros</a>
      </div>
      @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
      @if (loading()) {
        <p class="market-message" role="status">Carregando eventos...</p>
      } @else if (events().length === 0) {
        <div class="market-empty"><h2>Nenhum evento cadastrado.</h2></div>
      } @else {
        <div class="producer-event-list">
          @for (event of events(); track event.id) {
            <article class="producer-event-row">
              <img [src]="event.imageUrl" [alt]="event.title" loading="lazy" />
              <div class="producer-event-info">
                <span class="event-location">{{ event.city }}, {{ event.stateCode }}</span>
                <h2>{{ event.title }}</h2>
                <p>{{ formatDate(event.startsAt) }}</p>
              </div>
              <span class="event-status" [class]="'event-status status-' + event.status.toLowerCase()">
                {{ statusLabel(event.status) }}
              </span>
              <div class="producer-event-actions">
                @if (event.status === 'PUBLISHED') {
                  <button type="button" class="button-secondary" [disabled]="workingId() === event.id" (click)="changeStatus(event, 'suspender')">Suspender</button>
                }
                @if (event.status !== 'CANCELLED') {
                  <button type="button" class="button-danger" [disabled]="workingId() === event.id" (click)="changeStatus(event, 'cancelar')">Cancelar</button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class AdminEvents implements OnInit {
  private readonly api = inject(EventApi);
  protected readonly events = signal<EventSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly workingId = signal<number | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.listAdmin().subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Esta conta não tem permissão de administração.'
          : 'Não foi possível carregar os eventos. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  protected changeStatus(event: EventSummary, action: 'suspender' | 'cancelar'): void {
    if (action === 'cancelar' && !globalThis.confirm(`Cancelar “${event.title}”? Ingressos pagos e não utilizados terão reembolso simulado; ingressos já usados não serão reembolsados por esta regra.`)) return;
    this.workingId.set(event.id);
    const request = action === 'suspender' ? this.api.suspend(event.id) : this.api.cancel(event.id);
    request.subscribe({
      next: () => {
        this.workingId.set(null);
        this.load();
      },
      error: () => {
        this.error.set('Não foi possível alterar o evento. Atualize a lista e tente novamente.');
        this.workingId.set(null);
      },
    });
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    }).format(new Date(value));
  }

  protected statusLabel(status: EventStatus): string {
    return status === 'PUBLISHED' ? 'Publicado' : status === 'SUSPENDED' ? 'Suspenso' : 'Cancelado';
  }
}