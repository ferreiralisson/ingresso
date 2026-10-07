import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { EventApi, EventSummary } from '../core/event-api';
import { AuthService } from '../core/auth.service';

@Component({
  imports: [RouterLink],
  template: `
    <section class="market-page">
      <div class="market-heading">
        <div>
          <span class="section-kicker">AGENDA</span>
          <h1>Encontre seu próximo evento<span class="accent">.</span></h1>
          <p class="market-intro">Experiências para viver de perto.</p>
        </div>
        @if (auth.session()) {
          <a class="market-action" routerLink="/produtor/eventos">Área do produtor <span>↗</span></a>
        }
      </div>

      @if (loading()) {
        <p class="market-message" role="status">Carregando eventos...</p>
      } @else if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      } @else if (events().length === 0) {
        <div class="market-empty">
          <span class="section-kicker">NOVIDADES EM BREVE</span>
          <h2>A agenda está sendo preparada.</h2>
          <p>Volte em breve para descobrir novos eventos.</p>
        </div>
      } @else {
        <div class="event-grid">
          @for (event of events(); track event.id) {
            <a class="event-card" [routerLink]="['/eventos', event.id]">
              <img [src]="event.imageUrl" [alt]="event.title" loading="lazy" />
              <div class="event-card-body">
                <span class="event-location">{{ event.city }}, {{ event.stateCode }}</span>
                <h2>{{ event.title }}</h2>
                <p>{{ formatDate(event.startsAt) }}</p>
                <span class="event-price">
                  {{ event.lowestPriceInCents === 0 ? 'Gratuito' : 'A partir de ' + formatPrice(event.lowestPriceInCents) }}
                  <span aria-hidden="true">↗</span>
                </span>
              </div>
            </a>
          }
        </div>
        <nav class="market-pagination" aria-label="Paginação de eventos">
          <button type="button" class="button-secondary" [disabled]="page() === 0" (click)="load(page() - 1)">
            Anterior
          </button>
          <span>Página {{ page() + 1 }} de {{ totalPages() }}</span>
          <button type="button" class="button-secondary" [disabled]="page() + 1 >= totalPages()" (click)="load(page() + 1)">
            Próxima
          </button>
        </nav>
      }
    </section>
  `,
})
export class Events implements OnInit {
  private readonly eventApi = inject(EventApi);
  protected readonly auth = inject(AuthService);
  protected readonly events = signal<EventSummary[]>([]);
  protected readonly page = signal(0);
  protected readonly totalPages = signal(1);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.load(0);
  }

  protected load(page: number): void {
    this.loading.set(true);
    this.error.set('');
    this.eventApi.listPublic(page).subscribe({
      next: (result) => {
        this.events.set(result.content);
        this.page.set(result.number);
        this.totalPages.set(Math.max(1, result.totalPages));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 0
          ? 'Não foi possível conectar à agenda. Tente novamente.'
          : 'Não foi possível carregar os eventos. Tente novamente.');
        this.loading.set(false);
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

  protected formatPrice(cents: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
  }
}