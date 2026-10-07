import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventApi, EventDetail as EventDetailData } from '../core/event-api';
import { AuthService } from '../core/auth.service';

@Component({
  imports: [RouterLink],
  template: `
    <section class="market-page event-detail-page">
      <a class="text-link" routerLink="/eventos">← Voltar para eventos</a>

      @if (loading()) {
        <p class="market-message" role="status">Carregando evento...</p>
      } @else if (error()) {
        <div class="market-empty" role="alert">
          <span class="section-kicker">EVENTO INDISPONÍVEL</span>
          <h1>{{ error() }}</h1>
          <a class="button-secondary" routerLink="/eventos">Ver outros eventos</a>
        </div>
      } @else if (event(); as eventData) {
        @if (eventData.status === 'CANCELLED') {
          <div class="notice error event-cancelled" role="status">
            Este evento foi cancelado. Os ingressos não estão disponíveis.
          </div>
        }
        <article class="event-detail-hero">
          <img [src]="eventData.imageUrl" [alt]="eventData.title" />
          <div class="event-detail-copy">
            <span class="section-kicker">{{ eventData.venue.city }}, {{ eventData.venue.stateCode }}</span>
            <h1>{{ eventData.title }}<span class="accent">.</span></h1>
            <p class="event-detail-description">{{ eventData.description }}</p>
            <dl class="event-facts">
              <div><dt>Data e horário</dt><dd>{{ formatDate(eventData.startsAt) }}</dd></div>
              <div><dt>Local</dt><dd>{{ eventData.venue.name }}<br />{{ eventData.venue.streetAddress }}</dd></div>
              <div><dt>Organização</dt><dd>{{ eventData.organizer }}</dd></div>
              <div><dt>Classificação</dt><dd>{{ eventData.ageClassification }}</dd></div>
            </dl>
          </div>
        </article>

        <section class="event-offers-section" aria-labelledby="offers-title">
          <div class="event-section-heading">
            <div>
              <span class="section-kicker">INGRESSOS</span>
              <h2 id="offers-title">Escolha sua categoria</h2>
            </div>
            <span class="availability-note">Disponibilidade configurada</span>
          </div>
          <div class="offer-list">
            @for (category of eventData.categories; track category.id) {
              <article class="offer-row">
                <div>
                  <h3>{{ category.name }}</h3>
                  <p>{{ category.admissionMode === 'ASSIGNED_SEAT' ? 'Lugar marcado' : 'Entrada sem lugar marcado' }}</p>
                </div>
                <div class="offer-availability">
                  <strong>{{ category.availableQuantity }} disponíveis</strong>
                  <span>{{ category.priceInCents === 0 ? 'Gratuito' : formatPrice(category.priceInCents) }}</span>
                </div>
              </article>
            }
          </div>
          @if (eventData.status === 'PUBLISHED' && eventData.categories.some(category => category.availableQuantity > 0)) {
            @if (isSignedIn()) {
              <a class="market-action checkout-start" [routerLink]="['/eventos', eventData.id, 'comprar']">
                Continuar para o checkout <span>↗</span>
              </a>
            } @else {
              <a class="market-action checkout-start" [routerLink]="['/entrar']">
                Entre para continuar <span>↗</span>
              </a>
            }
          }
        </section>

        @if (eventData.sectors.length) {
          <section class="seat-map-section" aria-labelledby="seat-map-title">
            <div class="event-section-heading">
              <div>
                <span class="section-kicker">MAPA DO EVENTO</span>
                <h2 id="seat-map-title">Lugares disponíveis</h2>
              </div>
            </div>
            @for (sector of eventData.sectors; track sector.id) {
              <section class="seat-sector">
                <h3>{{ sector.name }}</h3>
                @for (row of sector.rows; track row.id) {
                  <div class="seat-row">
                    <span class="seat-row-label">{{ row.label }}</span>
                    <div class="seat-grid">
                      @for (seat of row.seats; track seat.id) {
                        <button
                          type="button"
                          class="seat-tile"
                          [class.seat-unavailable]="seat.status !== 'AVAILABLE'"
                          [disabled]="seat.status !== 'AVAILABLE' || eventData.status !== 'PUBLISHED'"
                          [attr.aria-label]="'Assento ' + sector.name + ', fileira ' + row.label + ', número ' + seat.label + ', ' + seat.categoryName + ', ' + formatPrice(seat.priceInCents)"
                          [title]="seat.categoryName + ' · ' + formatPrice(seat.priceInCents)"
                        >{{ seat.label }}</button>
                      }
                    </div>
                  </div>
                }
              </section>
            }
          </section>
        }
      }
    </section>
  `,
})
export class EventDetail implements OnInit {
  private readonly api = inject(EventApi);
  private readonly route = inject(ActivatedRoute);
  protected readonly auth = inject(AuthService);
  protected readonly event = signal<EventDetailData | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected isSignedIn(): boolean {
    const session = this.auth.session();
    return !!session && session.expiresAt > Date.now();
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id < 1) {
      this.error.set('Não foi possível encontrar este evento.');
      this.loading.set(false);
      return;
    }
    this.api.getPublic(id).subscribe({
      next: (event) => {
        this.event.set(event);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set('Não foi possível encontrar este evento.');
        this.loading.set(false);
      },
    });
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'full',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    }).format(new Date(value));
  }

  protected formatPrice(cents: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
  }
}