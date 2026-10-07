import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventApi, EventDetail } from '../core/event-api';
import { OrderApi, OrderResponse, OrderStatus } from '../core/order-api';

@Component({
  imports: [RouterLink],
  template: `
    <section class="market-page checkout-page">
      <a class="text-link" [routerLink]="['/eventos', eventId()]">← Voltar ao evento</a>
      <div class="market-heading">
        <div>
          <span class="section-kicker">CHECKOUT</span>
          <h1>Seu pedido<span class="accent">.</span></h1>
          @if (event(); as eventData) {
            <p class="market-intro">{{ eventData.title }} · {{ eventData.venue.city }}, {{ eventData.venue.stateCode }}</p>
          }
        </div>
      </div>

      @if (loading()) {
        <p class="market-message" role="status">Carregando disponibilidade...</p>
      } @else if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      } @else if (order(); as orderData) {
        <section class="order-confirmation" aria-live="polite">
          <span class="section-kicker">PEDIDO #{{ orderData.id }}</span>
          <h2>{{ statusTitle(orderData.status) }}</h2>
          <p>{{ statusDescription(orderData) }}</p>
          <dl class="order-summary-facts">
            <div><dt>Evento</dt><dd>{{ orderData.eventTitle }}</dd></div>
            <div><dt>Total</dt><dd>{{ formatPrice(orderData.totalInCents) }}</dd></div>
            <div><dt>Status</dt><dd>{{ statusLabel(orderData.status) }}</dd></div>
            @if (orderData.status === 'PENDING_PAYMENT') {
              <div><dt>Reserva válida até</dt><dd>{{ formatDate(orderData.reservationExpiresAt) }}</dd></div>
            }
          </dl>
          @if (orderData.status === 'PENDING_PAYMENT') {
            <button type="button" class="button-secondary" [disabled]="refreshing()" (click)="refreshOrder()">
              {{ refreshing() ? 'Consultando…' : 'Atualizar status' }}
            </button>
          }
          <a class="button-primary order-history-link" routerLink="/pedidos">Ver meus pedidos <span>↗</span></a>
        </section>
      } @else if (event(); as eventData) {
        @if (eventData.status !== 'PUBLISHED') {
          <div class="notice error" role="alert">Este evento não está aceitando pedidos.</div>
        } @else {
          <div class="checkout-layout">
            <section class="checkout-selection" aria-labelledby="selection-title">
              <div class="event-section-heading">
                <div>
                  <span class="section-kicker">OFERTAS</span>
                  <h2 id="selection-title">Selecione seus ingressos</h2>
                </div>
                <span class="availability-note">Até 10 por categoria</span>
              </div>
              @for (category of eventData.categories; track category.id) {
                <article class="checkout-category">
                  <div class="checkout-category-heading">
                    <div>
                      <h3>{{ category.name }}</h3>
                      <p>{{ category.admissionMode === 'ASSIGNED_SEAT' ? 'Lugar marcado' : 'Entrada geral' }}</p>
                    </div>
                    <strong>{{ formatPrice(category.priceInCents) }}</strong>
                  </div>
                  @if (category.admissionMode === 'GENERAL_ADMISSION') {
                    <div class="quantity-control">
                      <span>{{ category.availableQuantity }} disponíveis</span>
                      <div class="stepper" [attr.aria-label]="'Quantidade de ' + category.name">
                        <button type="button" [disabled]="quantity(category.id) <= 0" (click)="setQuantity(category.id, quantity(category.id) - 1)">−</button>
                        <output>{{ quantity(category.id) }}</output>
                        <button type="button" [disabled]="quantity(category.id) >= 10 || quantity(category.id) >= category.availableQuantity" (click)="setQuantity(category.id, quantity(category.id) + 1)">＋</button>
                      </div>
                    </div>
                  } @else {
                    @for (sector of eventData.sectors; track sector.id) {
                      <section class="checkout-seat-sector">
                        <h4>{{ sector.name }}</h4>
                        @for (row of sector.rows; track row.id) {
                          <div class="seat-row">
                            <span class="seat-row-label">{{ row.label }}</span>
                            <div class="seat-grid">
                              @for (seat of row.seats; track seat.id) {
                                @if (seat.categoryCode === category.code) {
                                  <button
                                    type="button"
                                    class="seat-tile"
                                    [class.seat-selected]="isSeatSelected(seat.id)"
                                    [class.seat-unavailable]="seat.status !== 'AVAILABLE'"
                                    [disabled]="seat.status !== 'AVAILABLE' || (!isSeatSelected(seat.id) && selectedSeatCount(category.code) >= 10)"
                                    [attr.aria-pressed]="isSeatSelected(seat.id)"
                                    [attr.aria-label]="'Assento ' + sector.name + ', fileira ' + row.label + ', número ' + seat.label"
                                    (click)="toggleSeat(seat.id, category.code)"
                                  >{{ seat.label }}</button>
                                }
                              }
                            </div>
                          </div>
                        }
                      </section>
                    }
                    <div class="quantity-control">
                      <span>{{ selectedSeatCount(category.code) }} de até 10 assentos</span>
                    </div>
                  }
                </article>
              }
            </section>

            <aside class="checkout-summary" aria-labelledby="summary-title">
              <span class="section-kicker">RESUMO</span>
              <h2 id="summary-title">{{ eventData.title }}</h2>
              @for (line of summaryLines(); track line.code) {
                <div class="checkout-summary-line">
                  <span>{{ line.name }} × {{ line.quantity }}</span>
                  <strong>{{ formatPrice(line.total) }}</strong>
                </div>
              }
              <div class="checkout-total"><span>Total</span><strong>{{ formatPrice(totalInCents()) }}</strong></div>
              @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
              <button class="button-primary" type="button" [disabled]="submitting() || totalQuantity() === 0" (click)="submitOrder()">
                {{ submitting() ? 'Processando…' : 'Confirmar pedido' }} <span aria-hidden="true">{{ submitting() ? '◌' : '↗' }}</span>
              </button>
              <p class="checkout-note">A reserva dura até 15 minutos. Nenhuma cobrança real é realizada neste ambiente.</p>
            </aside>
          </div>
        }
      }
    </section>
  `,
})
export class Checkout implements OnInit {
  private readonly eventApi = inject(EventApi);
  private readonly orderApi = inject(OrderApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly eventId = signal(0);
  protected readonly event = signal<EventDetail | null>(null);
  protected readonly order = signal<OrderResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly refreshing = signal(false);
  protected readonly error = signal('');
  protected readonly quantities = signal<Record<number, number>>({});
  protected readonly selectedSeats = signal<Record<string, number[]>>({});
  private idempotencyKey = globalThis.crypto.randomUUID();
  private lastPayload = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id < 1) {
      this.error.set('Não foi possível encontrar este evento.');
      this.loading.set(false);
      return;
    }
    this.eventId.set(id);
    this.eventApi.getPublic(id).subscribe({
      next: (event) => {
        this.event.set(event);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Não foi possível encontrar este evento.');
        this.loading.set(false);
      },
    });
  }

  protected quantity(categoryId: number): number {
    return this.quantities()[categoryId] ?? 0;
  }

  protected setQuantity(categoryId: number, quantity: number): void {
    const next = Math.max(0, Math.min(10, Math.floor(quantity)));
    this.quantities.update((current) => ({ ...current, [categoryId]: next }));
    this.resetIdempotencyKey();
  }

  protected isSeatSelected(seatId: number): boolean {
    return Object.values(this.selectedSeats()).some((seats) => seats.includes(seatId));
  }

  protected selectedSeatCount(categoryCode: string): number {
    return this.selectedSeats()[categoryCode]?.length ?? 0;
  }

  protected toggleSeat(seatId: number, categoryCode: string): void {
    this.selectedSeats.update((current) => {
      const selected = new Set(current[categoryCode] ?? []);
      if (selected.has(seatId)) selected.delete(seatId);
      else if (selected.size < 10) selected.add(seatId);
      return { ...current, [categoryCode]: [...selected] };
    });
    this.resetIdempotencyKey();
  }

  protected summaryLines(): Array<{ code: string; name: string; quantity: number; total: number }> {
    const event = this.event();
    if (!event) return [];
    return event.categories.flatMap((category) => {
      const quantity = category.admissionMode === 'GENERAL_ADMISSION'
        ? this.quantity(category.id)
        : this.selectedSeatCount(category.code);
      return quantity ? [{ code: category.code, name: category.name, quantity, total: category.priceInCents * quantity }] : [];
    });
  }

  protected totalQuantity(): number {
    return this.summaryLines().reduce((total, line) => total + line.quantity, 0);
  }

  protected totalInCents(): number {
    return this.summaryLines().reduce((total, line) => total + line.total, 0);
  }

  protected submitOrder(): void {
    const event = this.event();
    if (!event || this.submitting()) return;
    const items = event.categories.flatMap((category) => {
      if (category.admissionMode === 'GENERAL_ADMISSION') {
        const quantity = this.quantity(category.id);
        return quantity > 0 ? [{ categoryId: category.id, quantity }] : [];
      }
      const seatIds = this.selectedSeats()[category.code] ?? [];
      return seatIds.length ? [{ categoryId: category.id, quantity: seatIds.length, seatIds }] : [];
    });
    if (!items.length) {
      this.error.set('Selecione ao menos um ingresso ou assento.');
      return;
    }
    const payload = { eventId: event.id, items };
    const serialized = JSON.stringify(payload);
    if (this.lastPayload && this.lastPayload !== serialized) this.idempotencyKey = globalThis.crypto.randomUUID();
    this.lastPayload = serialized;
    this.submitting.set(true);
    this.error.set('');
    this.orderApi.create(payload, this.idempotencyKey).subscribe({
      next: (order) => this.beginPayment(order.id),
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 0
          ? 'Não foi possível conectar ao serviço de pedidos. Tente novamente.'
          : error instanceof HttpErrorResponse && typeof error.error?.message === 'string'
            ? error.error.message
            : 'Não foi possível criar o pedido. Atualize a disponibilidade e tente novamente.');
        this.submitting.set(false);
      },
    });
  }

  protected refreshOrder(): void {
    const order = this.order();
    if (!order || this.refreshing()) return;
    this.refreshing.set(true);
    this.orderApi.getMine(order.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.refreshing.set(false);
      },
      error: () => {
        this.error.set('Não foi possível atualizar o pedido. Tente novamente.');
        this.refreshing.set(false);
      },
    });
  }

  protected formatPrice(cents: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    }).format(new Date(value));
  }

  protected statusLabel(status: OrderStatus): string {
    return status === 'PAID' ? 'Pago' : status === 'PENDING_PAYMENT' ? 'Pagamento pendente'
      : status === 'PAYMENT_DECLINED' ? 'Pagamento recusado' : status === 'EXPIRED' ? 'Reserva expirada' : 'Cancelado';
  }

  protected statusTitle(status: OrderStatus): string {
    return status === 'PAID' ? 'Pedido confirmado' : status === 'PENDING_PAYMENT' ? 'Pagamento em processamento'
      : status === 'PAYMENT_DECLINED' ? 'Pagamento recusado' : status === 'EXPIRED' ? 'Reserva expirada' : 'Pedido cancelado';
  }

  protected statusDescription(order: OrderResponse): string {
    if (order.status === 'PAID') return 'Seu pedido foi aprovado. A emissão do ingresso digital acontece na etapa de entrega.';
    if (order.status === 'PENDING_PAYMENT') return 'O pedido está pendente. Atualize o status antes do fim do prazo de reserva.';
    if (order.status === 'PAYMENT_DECLINED') return 'O pagamento foi recusado e a disponibilidade foi liberada.';
    if (order.status === 'EXPIRED') return 'O prazo terminou e a disponibilidade foi liberada.';
    return 'Este pedido foi encerrado e não pode ser pago.';
  }

  private beginPayment(orderId: number): void {
    this.orderApi.startPayment(orderId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.submitting.set(false);
      },
      error: () => {
        this.error.set('O pedido foi criado, mas não foi possível iniciar o pagamento simulado. Consulte seus pedidos.');
        this.submitting.set(false);
      },
    });
  }

  private resetIdempotencyKey(): void {
    if (this.lastPayload) this.idempotencyKey = globalThis.crypto.randomUUID();
  }
}