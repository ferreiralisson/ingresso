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
          <span class="section-kicker">PRODUTOR</span>
          <h1>Meus eventos<span class="accent">.</span></h1>
          <p class="market-intro">Gerencie a agenda e as ofertas dos seus eventos.</p>
        </div>
        <a class="market-action" routerLink="/produtor/eventos/novo">Criar evento <span>＋</span></a>
      </div>

      @if (loading()) {
        <p class="market-message" role="status">Carregando seus eventos...</p>
      } @else if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      } @else if (events().length === 0) {
        <div class="market-empty">
          <span class="section-kicker">SUA AGENDA</span>
          <h2>Seu próximo evento começa aqui.</h2>
          <p>Crie um evento e publique as primeiras categorias.</p>
          <a class="button-primary event-cta" routerLink="/produtor/eventos/novo">Criar evento <span>↗</span></a>
        </div>
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
                <a class="text-link" [routerLink]="['/produtor/eventos', event.id, 'financeiro']">Financeiro</a>
                <a class="button-secondary" [routerLink]="['/produtor/eventos', event.id, 'editar']">Editar</a>
                @if (event.status === 'SUSPENDED') {
                  <button type="button" class="button-secondary" [disabled]="workingId() === event.id" (click)="reactivate(event)">
                    Reativar
                  </button>
                }
                @if (event.status === 'PUBLISHED') {
                  <a class="text-link" [routerLink]="['/eventos', event.id]">Ver público ↗</a>
                  <a class="button-secondary" [routerLink]="['/eventos', event.id, 'entrada']">Entrada</a>
                }
                @if (event.status !== 'CANCELLED') {
                  <button type="button" class="button-danger" [disabled]="workingId() === event.id" (click)="cancel(event)">Cancelar</button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class ProducerEvents implements OnInit {
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
    this.api.listProducer().subscribe({
      next: (page) => {
        this.events.set(page.content);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Esta conta não tem permissão de produtor.'
          : 'Não foi possível carregar os eventos. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  protected reactivate(event: EventSummary): void {
    this.workingId.set(event.id);
    this.api.reactivate(event.id).subscribe({
      next: () => {
        this.workingId.set(null);
        this.load();
      },
      error: () => {
        this.error.set('Não foi possível reativar o evento. Atualize a lista e tente novamente.');
        this.workingId.set(null);
      },
    });
  }

  protected cancel(event: EventSummary): void {
    if (!globalThis.confirm(`Cancelar “${event.title}”? Ingressos pagos e não utilizados terão reembolso simulado; ingressos já usados não serão reembolsados por esta regra.`)) return;
    this.workingId.set(event.id);
    this.api.cancelOwned(event.id).subscribe({
      next: () => {
        this.workingId.set(null);
        this.load();
      },
      error: () => {
        this.error.set('Não foi possível cancelar o evento. Atualize a lista e tente novamente.');
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